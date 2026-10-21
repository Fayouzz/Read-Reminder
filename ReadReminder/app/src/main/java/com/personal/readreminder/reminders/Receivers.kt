package com.personal.readreminder.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.readreminder.AppContainer
import com.personal.readreminder.ReadReminderApp
import com.personal.readreminder.data.SessionInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun container(ctx: Context): AppContainer = (ctx.applicationContext as ReadReminderApp).container

/** Alarm delivery and the notification buttons. Never logs pages unless the user taps "I read". */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = container(context)
        val result = goAsync()
        c.appScope.launch {
            try { handle(context, c, intent) } finally { result.finish() }
        }
    }

    private suspend fun handle(ctx: Context, c: AppContainer, intent: Intent) {
        val s = c.settings.settings.first()
        val now = System.currentTimeMillis()
        when (intent.action) {
            ReminderActions.FIRE -> {
                c.settings.clearPending()
                val kind = intent.getStringExtra(ReminderActions.EXTRA_KIND) ?: "SCHEDULED"
                val message = intent.getStringExtra(ReminderActions.EXTRA_MESSAGE) ?: "Time to read a few pages!"
                val pages = intent.getIntExtra(ReminderActions.EXTRA_PAGES, s.defaultTargetPages)
                val day = c.repository.logicalToday().toEpochDay()
                val done = c.db.sessionDao().pagesOnDay(day)
                val goal = c.db.goalDao().get(day)?.targetPages ?: s.dailyGoal
                val goalReached = goal > 0 && done >= goal
                if (s.remindersEnabled && !(s.stopAfterGoal && goalReached)) {
                    val shown = NotificationHelper.show(ctx, s, message, pages)
                    // One follow-up only, and only after a normal scheduled reminder that was shown.
                    if (shown && kind == "SCHEDULED" && s.remindMissed) {
                        c.settings.setPending(now + 30 * 60_000L, "Still time to read \u2014 a few pages count!", "FOLLOWUP")
                    }
                }
            }
            ReminderActions.SNOOZE -> {
                NotificationHelper.cancel(ctx)
                c.settings.setPending(now + s.snoozeMinutes * 60_000L, "Snooze is over \u2014 ready to read?", "SNOOZE")
            }
            ReminderActions.SKIP -> { NotificationHelper.cancel(ctx); c.settings.clearPending() }
            ReminderActions.READ -> {
                NotificationHelper.cancel(ctx); c.settings.clearPending()
                val pages = intent.getIntExtra(ReminderActions.EXTRA_PAGES, s.defaultTargetPages)
                if (pages > 0) c.repository.addSession(SessionInput(s.defaultBookId.takeIf { it > 0 }, pages, null, "Logged from reminder"))
            }
        }
        c.scheduler.reschedule()
    }
}

/** Re-arms the alarm after reboot, app update, clock/time-zone change, or exact-alarm permission change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = container(context)
        val result = goAsync()
        c.appScope.launch {
            try { c.scheduler.reschedule() } finally { result.finish() }
        }
    }
}
