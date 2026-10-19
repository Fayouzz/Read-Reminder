package com.personal.readreminder.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

class ReadingRepository(
    private val sessions: SessionDao,
    private val goals: GoalDao,
    private val settings: SettingsRepository,
) {
    fun observeSessions(day: LocalDate): Flow<List<ReadingSessionEntity>> = sessions.observeDay(day.toEpochDay())
    fun observeTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = sessions.observeTotals(from.toEpochDay(), to.toEpochDay())
    fun observeGoals(from: LocalDate, to: LocalDate): Flow<List<DailyGoalEntity>> = goals.observeRange(from.toEpochDay(), to.toEpochDay())

    suspend fun logPages(pages: Int, day: LocalDate, now: Instant, bookId: Long? = null, notes: String? = null) {
        require(pages > 0) { "Pages must be positive (zero-page timed sessions arrive in Stage 2)" }
        val s = settings.settings.first()
        goals.insertIfAbsent(DailyGoalEntity(day.toEpochDay(), s.dailyGoal, s.resetMinutes)) // snapshot, never overwritten
        val t = now.toEpochMilli()
        sessions.insert(ReadingSessionEntity(bookId = bookId, sessionDate = day.toEpochDay(), startTimestamp = t,
            pagesRead = pages, notes = notes, createdAt = t, updatedAt = t))
    }

    suspend fun delete(s: ReadingSessionEntity) = sessions.delete(s)

    /** Changes the default and today's snapshot only; earlier days keep their own goal. */
    suspend fun setDailyGoal(pages: Int, today: LocalDate) {
        settings.setDailyGoal(pages)
        goals.upsert(DailyGoalEntity(today.toEpochDay(), pages, settings.settings.first().resetMinutes))
    }
}
