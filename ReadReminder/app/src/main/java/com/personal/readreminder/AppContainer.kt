package com.personal.readreminder

import android.app.Application
import com.personal.readreminder.data.*
import com.personal.readreminder.domain.AchievementEvaluator
import com.personal.readreminder.domain.AchievementType
import com.personal.readreminder.reminders.NotificationHelper
import com.personal.readreminder.reminders.ReminderScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

/** Simple manual DI container (no Hilt needed at this size). */
class AppContainer(val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.build(app)
    val settings = SettingsRepository(app)
    val repository = ReadingRepository(db.sessionDao(), db.goalDao(), db.bookDao(), db.reminderDao(), db.achievementDao(), settings)
    val scheduler = ReminderScheduler(app, db, settings)
    val backup = BackupManager(db, settings)

    /** Newly unlocked achievements (each is announced once: the database insert decides). */
    val achievementEvents = MutableSharedFlow<AchievementType>(extraBufferCapacity = 16)

    /** Six editable suggestions, created once on first launch. */
    suspend fun seedDefaults() {
        if (settings.settings.first().seeded) return
        listOf(7 * 60 + 30, 9 * 60 + 30, 12 * 60 + 30, 16 * 60 + 30, 19 * 60 + 30, 21 * 60).forEach { m ->
            db.reminderDao().insert(ReminderEntity(hour = m / 60, minute = m % 60))
        }
        settings.set(K.seeded, true)
    }

    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch { seedDefaults() }
        appScope.launch { settings.settings.collect { NotificationHelper.ensureChannel(app, it) } }
        // Any change to settings, reminders or reading sessions re-plans the single alarm.
        appScope.launch {
            combine(settings.settings, db.reminderDao().observeAll(), db.sessionDao().observeAll()) { _, _, _ -> Unit }
                .debounce(400).collect { scheduler.reschedule() }
        }
        // Achievements are computed from real data; the insert result prevents duplicate announcements.
        appScope.launch {
            combine(db.sessionDao().observeAll(), db.goalDao().observeAll(), db.bookDao().observeAll(), settings.settings) { sessions, goals, books, s ->
                AchievementEvaluator.satisfied(AchievementEvaluator.stats(sessions, goals, books, s.dailyGoal, LocalDate.now()))
            }.collect { earned ->
                earned.forEach { t ->
                    if (db.achievementDao().insertIfAbsent(AchievementEntity(t.name, System.currentTimeMillis())) != -1L) achievementEvents.tryEmit(t)
                }
            }
        }
    }
}
