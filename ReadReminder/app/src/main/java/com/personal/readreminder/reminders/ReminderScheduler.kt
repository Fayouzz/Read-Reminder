package com.personal.readreminder.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.personal.readreminder.data.AppDatabase
import com.personal.readreminder.data.SettingsRepository
import com.personal.readreminder.domain.DayBoundary
import com.personal.readreminder.domain.Pending
import com.personal.readreminder.domain.PendingKind
import com.personal.readreminder.domain.ReminderPlanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Keeps exactly one alarm armed (always the same PendingIntent), so duplicates are impossible.
 * Uses an exact alarm when Android allows it and falls back to an inexact one otherwise.
 */
class ReminderScheduler(private val context: Context, private val db: AppDatabase, private val settings: SettingsRepository) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val mutex = Mutex()

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    private fun pendingIntent(message: String, pages: Int, kind: String, create: Boolean): PendingIntent? {
        val i = Intent(context, ReminderReceiver::class.java).setAction(ReminderActions.FIRE)
            .putExtra(ReminderActions.EXTRA_MESSAGE, message).putExtra(ReminderActions.EXTRA_PAGES, pages).putExtra(ReminderActions.EXTRA_KIND, kind)
        val flags = PendingIntent.FLAG_IMMUTABLE or (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, 7001, i, flags)
    }

    suspend fun reschedule() = mutex.withLock {
        val s = settings.settings.first()
        val rt = settings.runtime.first()
        val now = ZonedDateTime.now()
        if (rt.pendingAt in 1..now.toInstant().toEpochMilli()) settings.clearPending()

        val today = DayBoundary.logicalDate(now.toInstant(), now.zone, s.resetMinutes)
        val pages = db.sessionDao().pagesOnDay(today.toEpochDay())
        val goal = db.goalDao().get(today.toEpochDay())?.targetPages ?: s.dailyGoal
        val goalDay = if (goal > 0 && pages >= goal) today else null
        val last = db.sessionDao().lastCreatedAt()?.let { Instant.ofEpochMilli(it) }
        val pending = if (rt.pendingAt > now.toInstant().toEpochMilli())
            Pending(Instant.ofEpochMilli(rt.pendingAt), rt.pendingMessage, if (rt.pendingKind == PendingKind.SNOOZE.name) PendingKind.SNOOZE else PendingKind.FOLLOWUP)
        else null

        val next = ReminderPlanner.next(db.reminderDao().getAllOnce(), s, now, goalDay, last, pending)
        if (next == null) {
            pendingIntent("", 0, "", false)?.let { am.cancel(it); it.cancel() }
            settings.setNext(0L, "")
            return@withLock
        }
        val pi = pendingIntent(next.message, next.targetPages, next.kind, true)!!
        val at = next.at.toInstant().toEpochMilli()
        try {
            if (canScheduleExact()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)   // exact access revoked meanwhile
        }
        settings.setNext(at, next.message)
    }
}
