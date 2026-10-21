package com.personal.readreminder.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.personal.readreminder.MainActivity
import com.personal.readreminder.R
import com.personal.readreminder.data.AppSettings
import com.personal.readreminder.data.NotifTone

object ReminderActions {
    const val FIRE = "com.personal.readreminder.FIRE"
    const val SNOOZE = "com.personal.readreminder.SNOOZE"
    const val SKIP = "com.personal.readreminder.SKIP"
    const val READ = "com.personal.readreminder.READ"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_PAGES = "pages"
    const val EXTRA_KIND = "kind"
}

object NotificationHelper {
    const val NOTIF_ID = 4242
    private const val PREFIX = "reminders_"

    /**
     * Android freezes a channel's sound/vibration after creation, so the channel id encodes the settings;
     * when the user changes them a new channel is created and the stale one removed.
     */
    fun ensureChannel(ctx: Context, s: AppSettings): String {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val loud = s.alertEnabled && s.tone != NotifTone.SILENT
        val id = PREFIX + s.tone.name + "_" + (if (s.vibrate && s.alertEnabled) 1 else 0) + "_" + (if (s.alertEnabled) 1 else 0)
        if (nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, "Reading reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Reminders to read and log pages"
            if (loud) {
                val type = when (s.tone) {
                    NotifTone.ALARM -> RingtoneManager.TYPE_ALARM
                    NotifTone.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                    else -> RingtoneManager.TYPE_NOTIFICATION
                }
                ch.setSound(RingtoneManager.getDefaultUri(type), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            } else ch.setSound(null, null)
            ch.enableVibration(s.vibrate && s.alertEnabled)
            nm.createNotificationChannel(ch)
        }
        nm.notificationChannels.filter { it.id.startsWith(PREFIX) && it.id != id }.forEach { nm.deleteNotificationChannel(it.id) }
        return id
    }

    fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    private fun action(ctx: Context, action: String, code: Int, pages: Int): PendingIntent =
        PendingIntent.getBroadcast(ctx, code,
            Intent(ctx, ReminderReceiver::class.java).setAction(action).putExtra(ReminderActions.EXTRA_PAGES, pages),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Returns false (and shows nothing) when notifications are blocked. */
    fun show(ctx: Context, s: AppSettings, message: String, targetPages: Int): Boolean {
        if (!canPost(ctx)) return false
        val channel = ensureChannel(ctx, s)
        val open = PendingIntent.getActivity(ctx, 1,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle("Read Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message + "\nTarget for this session: $targetPages pages"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Snooze ${s.snoozeMinutes} min", action(ctx, ReminderActions.SNOOZE, 11, targetPages))
            .addAction(0, "I read $targetPages pages", action(ctx, ReminderActions.READ, 12, targetPages))
            .addAction(0, "Skip", action(ctx, ReminderActions.SKIP, 13, targetPages))
            .build()
        return try { NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n); true } catch (e: SecurityException) { false }
    }

    fun cancel(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
}
