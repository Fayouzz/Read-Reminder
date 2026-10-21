package com.personal.readreminder.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode(val label: String) { SYSTEM("System default"), LIGHT("Light"), DARK("Dark") }
enum class ReminderMode(val label: String) { FIXED("Fixed schedule"), INTERVAL("Interval"), ADAPTIVE("Adaptive") }
enum class NotifTone(val label: String) { DEFAULT("Default notification"), ALARM("Alarm"), RINGTONE("Ringtone"), SILENT("Silent") }
enum class MotivationStyle(val label: String) { GENTLE("Gentle"), CHEERFUL("Cheerful"), DIRECT("Direct") }

/** Times are stored as minutes after midnight. */
data class AppSettings(
    val dailyGoal: Int = 10,
    val resetMinutes: Int = 0,
    val displayName: String = "",
    // reminders
    val remindersEnabled: Boolean = true,
    val defaultMessage: String = "",
    val vibrate: Boolean = true,
    val alertEnabled: Boolean = true,
    val tone: NotifTone = NotifTone.DEFAULT,
    val mode: ReminderMode = ReminderMode.FIXED,
    val intervalMinutes: Int = 120,
    val activeStart: Int = 7 * 60,
    val activeEnd: Int = 22 * 60,
    val quietEnabled: Boolean = true,
    val quietStart: Int = 22 * 60 + 30,
    val quietEnd: Int = 6 * 60 + 30,
    val remindMissed: Boolean = true,
    val stopAfterGoal: Boolean = true,
    val reduceAfterLog: Boolean = true,
    val snoozeMinutes: Int = 15,
    val defaultTargetPages: Int = 10,
    // reading
    val preferredStart: Int = 19 * 60,
    val preferredEnd: Int = 21 * 60,
    val defaultBookId: Long = -1L,
    val defaultSessionMinutes: Int = 20,
    val weekStartMonday: Boolean = true,
    // display
    val dashboardMinutes: Boolean = false,
    val time24h: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val historyDefaultYear: Boolean = false,
    val compactRecords: Boolean = false,
    // personalization
    val motivation: MotivationStyle = MotivationStyle.CHEERFUL,
    val targetMessage: String = "",
    val seeded: Boolean = false,
)

/** Not part of the user's settings or backups: scheduler bookkeeping. */
data class ReminderRuntime(
    val nextAt: Long = 0L, val nextMessage: String = "",
    val pendingAt: Long = 0L, val pendingMessage: String = "", val pendingKind: String = "",
)

object K {
    val goal = intPreferencesKey("daily_goal")
    val reset = intPreferencesKey("reset_minutes")
    val name = stringPreferencesKey("display_name")
    val remEnabled = booleanPreferencesKey("reminders_enabled")
    val defMsg = stringPreferencesKey("default_message")
    val vibrate = booleanPreferencesKey("vibrate")
    val alert = booleanPreferencesKey("alert_enabled")
    val tone = stringPreferencesKey("tone")
    val mode = stringPreferencesKey("reminder_mode")
    val interval = intPreferencesKey("interval_minutes")
    val actStart = intPreferencesKey("active_start")
    val actEnd = intPreferencesKey("active_end")
    val quietOn = booleanPreferencesKey("quiet_enabled")
    val quietStart = intPreferencesKey("quiet_start")
    val quietEnd = intPreferencesKey("quiet_end")
    val missed = booleanPreferencesKey("remind_missed")
    val stopGoal = booleanPreferencesKey("stop_after_goal")
    val reduce = booleanPreferencesKey("reduce_after_log")
    val snooze = intPreferencesKey("snooze_minutes")
    val targetPages = intPreferencesKey("default_target_pages")
    val prefStart = intPreferencesKey("preferred_start")
    val prefEnd = intPreferencesKey("preferred_end")
    val defBook = longPreferencesKey("default_book")
    val defMinutes = intPreferencesKey("default_session_minutes")
    val weekMon = booleanPreferencesKey("week_start_monday")
    val metricMin = booleanPreferencesKey("dashboard_minutes")
    val time24 = booleanPreferencesKey("time_24h")
    val theme = stringPreferencesKey("theme")
    val histYear = booleanPreferencesKey("history_default_year")
    val compact = booleanPreferencesKey("compact_records")
    val motivation = stringPreferencesKey("motivation")
    val targetMsg = stringPreferencesKey("target_message")
    val seeded = booleanPreferencesKey("seeded")

    // transient (never backed up)
    val nextAt = longPreferencesKey("tmp_next_at")
    val nextMsg = stringPreferencesKey("tmp_next_msg")
    val pendAt = longPreferencesKey("tmp_pending_at")
    val pendMsg = stringPreferencesKey("tmp_pending_msg")
    val pendKind = stringPreferencesKey("tmp_pending_kind")

    val ints = listOf(goal, reset, interval, actStart, actEnd, quietStart, quietEnd, snooze, targetPages, prefStart, prefEnd, defMinutes).associateBy { it.name }
    val longs = listOf(defBook).associateBy { it.name }
    val bools = listOf(remEnabled, vibrate, alert, quietOn, missed, stopGoal, reduce, weekMon, metricMin, time24, histYear, compact, seeded).associateBy { it.name }
    val strings = listOf(name, defMsg, tone, mode, theme, motivation, targetMsg).associateBy { it.name }
}

private inline fun <reified T : Enum<T>> Preferences.enumOf(key: Preferences.Key<String>, def: T): T =
    this[key]?.let { v -> runCatching { enumValueOf<T>(v) }.getOrNull() } ?: def

class SettingsRepository(private val context: Context) {
    private val ds get() = context.dataStore

    val settings: Flow<AppSettings> = ds.data.map { p ->
        val d = AppSettings()
        AppSettings(
            dailyGoal = p[K.goal] ?: d.dailyGoal, resetMinutes = p[K.reset] ?: d.resetMinutes, displayName = p[K.name] ?: "",
            remindersEnabled = p[K.remEnabled] ?: d.remindersEnabled, defaultMessage = p[K.defMsg] ?: "",
            vibrate = p[K.vibrate] ?: d.vibrate, alertEnabled = p[K.alert] ?: d.alertEnabled,
            tone = p.enumOf(K.tone, d.tone), mode = p.enumOf(K.mode, d.mode),
            intervalMinutes = p[K.interval] ?: d.intervalMinutes,
            activeStart = p[K.actStart] ?: d.activeStart, activeEnd = p[K.actEnd] ?: d.activeEnd,
            quietEnabled = p[K.quietOn] ?: d.quietEnabled, quietStart = p[K.quietStart] ?: d.quietStart, quietEnd = p[K.quietEnd] ?: d.quietEnd,
            remindMissed = p[K.missed] ?: d.remindMissed, stopAfterGoal = p[K.stopGoal] ?: d.stopAfterGoal,
            reduceAfterLog = p[K.reduce] ?: d.reduceAfterLog, snoozeMinutes = p[K.snooze] ?: d.snoozeMinutes,
            defaultTargetPages = p[K.targetPages] ?: d.defaultTargetPages,
            preferredStart = p[K.prefStart] ?: d.preferredStart, preferredEnd = p[K.prefEnd] ?: d.preferredEnd,
            defaultBookId = p[K.defBook] ?: d.defaultBookId, defaultSessionMinutes = p[K.defMinutes] ?: d.defaultSessionMinutes,
            weekStartMonday = p[K.weekMon] ?: d.weekStartMonday,
            dashboardMinutes = p[K.metricMin] ?: d.dashboardMinutes, time24h = p[K.time24] ?: d.time24h,
            theme = p.enumOf(K.theme, d.theme), historyDefaultYear = p[K.histYear] ?: d.historyDefaultYear,
            compactRecords = p[K.compact] ?: d.compactRecords,
            motivation = p.enumOf(K.motivation, d.motivation), targetMessage = p[K.targetMsg] ?: "",
            seeded = p[K.seeded] ?: false,
        )
    }.distinctUntilChanged()

    val runtime: Flow<ReminderRuntime> = ds.data.map { p ->
        ReminderRuntime(p[K.nextAt] ?: 0L, p[K.nextMsg] ?: "", p[K.pendAt] ?: 0L, p[K.pendMsg] ?: "", p[K.pendKind] ?: "")
    }.distinctUntilChanged()

    suspend fun <T> set(key: Preferences.Key<T>, value: T) { ds.edit { it[key] = value } }

    suspend fun setDailyGoal(pages: Int) = set(K.goal, pages)

    suspend fun setNext(at: Long, message: String) { ds.edit { it[K.nextAt] = at; it[K.nextMsg] = message } }
    suspend fun setPending(at: Long, message: String, kind: String) {
        ds.edit { it[K.pendAt] = at; it[K.pendMsg] = message; it[K.pendKind] = kind }
    }
    suspend fun clearPending() { ds.edit { it[K.pendAt] = 0L; it[K.pendMsg] = ""; it[K.pendKind] = "" } }

    /** Values for backup: only known, non-transient keys. */
    suspend fun exportMap(): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        for ((k, v) in ds.data.first().asMap()) {
            val n = k.name
            if (n in K.ints || n in K.longs || n in K.bools || n in K.strings) out[n] = v
        }
        return out
    }

    suspend fun restore(map: Map<String, Any>) {
        ds.edit { p ->
            p.clear()
            for ((n, v) in map) {
                K.ints[n]?.let { k -> (v as? Number)?.let { p[k] = it.toInt() } }
                K.longs[n]?.let { k -> (v as? Number)?.let { p[k] = it.toLong() } }
                K.bools[n]?.let { k -> (v as? Boolean)?.let { p[k] = it } }
                K.strings[n]?.let { k -> (v as? String)?.let { p[k] = it } }
            }
        }
    }

    suspend fun clearAll() { ds.edit { it.clear() } }
}
