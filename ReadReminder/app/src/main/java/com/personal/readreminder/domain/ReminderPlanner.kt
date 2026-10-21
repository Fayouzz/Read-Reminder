package com.personal.readreminder.domain

import com.personal.readreminder.data.AppSettings
import com.personal.readreminder.data.ReminderEntity
import com.personal.readreminder.data.ReminderMode
import com.personal.readreminder.data.MotivationStyle
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class PendingKind { SNOOZE, FOLLOWUP }
data class Pending(val at: Instant, val message: String, val kind: PendingKind)
/** kind is "SCHEDULED", "SNOOZE" or "FOLLOWUP". */
data class PlannedReminder(val at: ZonedDateTime, val message: String, val targetPages: Int, val kind: String)

/**
 * Deterministic reminder planning. The app keeps exactly ONE alarm armed: the next reminder returned here.
 * Rules, in order: master switch -> candidate times (fixed / interval / adaptive) -> quiet hours ->
 * stop-after-goal -> skip shortly after a logged session. Snooze / follow-up can only bring the next alarm earlier.
 */
object ReminderPlanner {
    private data class Cand(val time: LocalTime, val message: String?, val target: Int?)

    fun isQuiet(minuteOfDay: Int, s: AppSettings): Boolean {
        if (!s.quietEnabled || s.quietStart == s.quietEnd) return false
        return if (s.quietStart < s.quietEnd) minuteOfDay in s.quietStart until s.quietEnd
        else (minuteOfDay >= s.quietStart || minuteOfDay < s.quietEnd)   // crosses midnight
    }

    private fun minutes(t: ZonedDateTime) = t.hour * 60 + t.minute
    private fun lt(m: Int): LocalTime { val x = ((m % 1440) + 1440) % 1440; return LocalTime.of(x / 60, x % 60) }

    private val POOLS = mapOf(
        MotivationStyle.CHEERFUL to listOf("Time to read a few pages!", "Take a short break and enjoy your book.", "Your reading goal is waiting for you.", "Just 10 pages can make a difference.", "Ready for your next reading session?"),
        MotivationStyle.GENTLE to listOf("Whenever you're ready, your book is here.", "A quiet few pages might feel good right now.", "No rush \u2014 a little reading goes a long way.", "Maybe a calm moment with your book?"),
        MotivationStyle.DIRECT to listOf("Read now. Hit today's page goal.", "Reading time. Open your book.", "Pages don't read themselves. Go.", "Goal check: have you read today?"),
    )

    fun pickMessage(s: AppSettings, day: LocalDate, time: LocalTime, custom: String?): String {
        if (!custom.isNullOrBlank()) return custom
        if (s.defaultMessage.isNotBlank()) return s.defaultMessage
        val pool = POOLS.getValue(s.motivation)
        return pool[((day.toEpochDay() + time.hour * 7 + time.minute).toInt() and Int.MAX_VALUE) % pool.size]
    }

    private fun candidates(day: LocalDate, reminders: List<ReminderEntity>, s: AppSettings, goalDay: LocalDate?): List<Cand> {
        val bit = 1 shl (day.dayOfWeek.value - 1)  // Monday = bit 0
        val fixed = reminders.filter { it.enabled && (it.weekdayMask and bit) != 0 }.map { Cand(LocalTime.of(it.hour, it.minute), it.message, it.targetPages) }
        return when (s.mode) {
            ReminderMode.FIXED -> fixed
            ReminderMode.INTERVAL -> {
                val step = maxOf(15, s.intervalMinutes)
                val start = s.activeStart
                var end = s.activeEnd
                if (end <= start) end += 1440      // overnight (or equal = whole day)
                val out = ArrayList<Cand>()
                var m = start
                while (m <= end && m < start + 1440) { out += Cand(lt(m), null, null); m += step }
                out
            }
            ReminderMode.ADAPTIVE -> {
                val lastCall = maxOf(0, s.activeEnd - 60)
                if (day != goalDay) fixed + Cand(lt(lastCall), "Last call: a few pages will keep your goal on track.", null) else fixed
            }
        }
    }

    fun nextScheduled(reminders: List<ReminderEntity>, s: AppSettings, now: ZonedDateTime, goalReachedDay: LocalDate?, lastActivity: Instant?): PlannedReminder? {
        if (!s.remindersEnabled) return null
        for (offset in 0..8L) {
            val day = now.toLocalDate().plusDays(offset)
            for (c in candidates(day, reminders, s, goalReachedDay).sortedBy { it.time }) {
                val at = ZonedDateTime.of(day, c.time, now.zone)
                if (!at.isAfter(now)) continue
                if (isQuiet(minutes(at), s)) continue
                val logical = DayBoundary.logicalDate(at.toInstant(), now.zone, s.resetMinutes)
                if (s.stopAfterGoal && logical == goalReachedDay) continue
                if ((s.reduceAfterLog || s.mode == ReminderMode.ADAPTIVE) && lastActivity != null) {
                    val gap = Duration.between(lastActivity, at.toInstant())
                    val window = if (s.mode == ReminderMode.ADAPTIVE) 90 else 60
                    if (!gap.isNegative && gap.toMinutes() < window) continue
                }
                return PlannedReminder(at, pickMessage(s, day, c.time, c.message), c.target ?: s.defaultTargetPages, "SCHEDULED")
            }
        }
        return null
    }

    fun next(reminders: List<ReminderEntity>, s: AppSettings, now: ZonedDateTime, goalReachedDay: LocalDate?, lastActivity: Instant?, pending: Pending?): PlannedReminder? {
        if (!s.remindersEnabled) return null
        val planned = nextScheduled(reminders, s, now, goalReachedDay, lastActivity)
        val p = pending?.takeIf { it.at.isAfter(now.toInstant()) }?.takeIf { pd ->
            val z = pd.at.atZone(now.zone)
            val logical = DayBoundary.logicalDate(pd.at, now.zone, s.resetMinutes)
            (pd.kind == PendingKind.SNOOZE || !isQuiet(minutes(z), s)) && !(s.stopAfterGoal && logical == goalReachedDay)
        }?.let { PlannedReminder(it.at.atZone(now.zone), it.message, s.defaultTargetPages, it.kind.name) }
        return listOfNotNull(planned, p).minByOrNull { it.at.toInstant() }
    }
}
