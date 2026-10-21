package com.personal.readreminder

import com.personal.readreminder.data.AppSettings
import com.personal.readreminder.data.ReminderEntity
import com.personal.readreminder.data.ReminderMode
import com.personal.readreminder.domain.Pending
import com.personal.readreminder.domain.PendingKind
import com.personal.readreminder.domain.ReminderPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val zone = ZoneId.of("UTC")
    private fun at(d: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 10, d, h, m, 0, 0, zone)   // 2026-10-19 is a Monday
    private fun r(h: Int, m: Int, mask: Int = 127, id: Long = 1) = ReminderEntity(id = id, hour = h, minute = m, weekdayMask = mask)
    private val s = AppSettings()

    @Test fun picksNextTimeToday() {
        val n = ReminderPlanner.nextScheduled(listOf(r(7, 30), r(9, 30, id = 2)), s, at(19, 8, 0), null, null)!!
        assertEquals(at(19, 9, 30), n.at)
    }

    @Test fun weekdayMaskSkipsUnselectedDays() {
        val mondayOnly = r(9, 0, mask = 1)   // bit 0 = Monday
        val n = ReminderPlanner.nextScheduled(listOf(mondayOnly), s, at(19, 10, 0), null, null)!!
        assertEquals(at(26, 9, 0), n.at)     // next Monday
    }

    @Test fun quietHoursCrossingMidnightAreSkipped() {
        // quiet 22:30 -> 06:30; 23:00 and 05:00 are quiet, 07:30 is fine
        val n = ReminderPlanner.nextScheduled(listOf(r(23, 0, id = 1), r(5, 0, id = 2), r(7, 30, id = 3)), s, at(19, 22, 0), null, null)!!
        assertEquals(at(20, 7, 30), n.at)
    }

    @Test fun quietHoursCanBeDisabled() {
        val n = ReminderPlanner.nextScheduled(listOf(r(23, 0)), s.copy(quietEnabled = false), at(19, 22, 0), null, null)!!
        assertEquals(at(19, 23, 0), n.at)
    }

    @Test fun stopsAfterGoalButResumesTomorrow() {
        val reminders = listOf(r(9, 30), r(20, 0, id = 2))
        val today = LocalDate.of(2026, 10, 19)
        val stop = ReminderPlanner.nextScheduled(reminders, s, at(19, 8, 0), today, null)!!
        assertEquals(at(20, 9, 30), stop.at)
        val keep = ReminderPlanner.nextScheduled(reminders, s.copy(stopAfterGoal = false), at(19, 8, 0), today, null)!!
        assertEquals(at(19, 9, 30), keep.at)
    }

    @Test fun intervalModeUsesActiveHours() {
        val cfg = s.copy(mode = ReminderMode.INTERVAL, intervalMinutes = 120, activeStart = 8 * 60, activeEnd = 12 * 60)
        assertEquals(at(19, 10, 0), ReminderPlanner.nextScheduled(emptyList(), cfg, at(19, 9, 30), null, null)!!.at)
        assertEquals(at(20, 8, 0), ReminderPlanner.nextScheduled(emptyList(), cfg, at(19, 12, 30), null, null)!!.at)
    }

    @Test fun adaptiveSkipsReminderSoonAfterReading() {
        val cfg = s.copy(mode = ReminderMode.ADAPTIVE)
        val last = at(19, 8, 50).toInstant()   // read 40 min before the 09:30 reminder
        val n = ReminderPlanner.nextScheduled(listOf(r(9, 30), r(12, 0, id = 2)), cfg, at(19, 8, 55), null, last)!!
        assertEquals(at(19, 12, 0), n.at)
    }

    @Test fun reduceAfterLogCanBeTurnedOff() {
        val last = at(19, 9, 0).toInstant()
        val n = ReminderPlanner.nextScheduled(listOf(r(9, 30)), s.copy(reduceAfterLog = false), at(19, 9, 10), null, last)!!
        assertEquals(at(19, 9, 30), n.at)
    }

    @Test fun snoozeBeatsScheduledReminder() {
        val pending = Pending(at(19, 8, 30).toInstant(), "snoozed", PendingKind.SNOOZE)
        val n = ReminderPlanner.next(listOf(r(9, 30)), s, at(19, 8, 0), null, null, pending)!!
        assertEquals(at(19, 8, 30), n.at); assertEquals("SNOOZE", n.kind)
    }

    @Test fun followUpIsSuppressedInQuietHours() {
        val pending = Pending(at(19, 23, 0).toInstant(), "follow", PendingKind.FOLLOWUP)
        val n = ReminderPlanner.next(listOf(r(7, 30)), s, at(19, 22, 40), null, null, pending)!!
        assertEquals(at(20, 7, 30), n.at); assertEquals("SCHEDULED", n.kind)
    }

    @Test fun masterSwitchOffMeansNoAlarm() {
        assertNull(ReminderPlanner.next(listOf(r(9, 30)), s.copy(remindersEnabled = false), at(19, 8, 0), null, null, null))
    }

    @Test fun disabledRemindersAreIgnored() {
        assertNull(ReminderPlanner.nextScheduled(listOf(r(9, 30).copy(enabled = false)), s, at(19, 8, 0), null, null))
    }

    @Test fun customMessageWinsOverDefaults() {
        val n = ReminderPlanner.nextScheduled(listOf(r(9, 30).copy(message = "Chapter time!")), s.copy(defaultMessage = "x"), at(19, 8, 0), null, null)!!
        assertEquals("Chapter time!", n.message)
    }
}
