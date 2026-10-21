package com.personal.readreminder.data

import com.personal.readreminder.domain.DayBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the user entered. pages may be 0 only for a timed session (durationMinutes > 0).
 * startTimestamp null = now. startPage/endPage are only set when entered as a page range (pages = end - start).
 */
data class SessionInput(
    val bookId: Long?, val pages: Int, val durationMinutes: Int?, val notes: String?,
    val startTimestamp: Long? = null, val startPage: Int? = null, val endPage: Int? = null,
) {
    val isValid: Boolean get() = pages >= 0 && (pages > 0 || (durationMinutes ?: 0) > 0)
}

/** currentPage is derived: starting page + pages from logged sessions, so it always matches history. */
data class BookProgress(val book: BookEntity, val currentPage: Int) {
    val fraction: Float? = book.totalPages?.takeIf { it > 0 }?.let { (currentPage.toFloat() / it).coerceIn(0f, 1f) }
    val reachedEnd: Boolean = book.totalPages?.let { it > 0 && currentPage >= it } ?: false
}

class ReadingRepository(
    private val sessions: SessionDao,
    private val goals: GoalDao,
    private val books: BookDao,
    private val reminders: ReminderDao,
    private val achievements: AchievementDao,
    private val settings: SettingsRepository,
) {
    private val zone get() = ZoneId.systemDefault()

    fun observeSessions(day: LocalDate): Flow<List<ReadingSessionEntity>> = sessions.observeDay(day.toEpochDay())
    fun observeAllSessions(): Flow<List<ReadingSessionEntity>> = sessions.observeAll()
    fun observeTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = sessions.observeTotals(from.toEpochDay(), to.toEpochDay())
    fun observeGoals(from: LocalDate, to: LocalDate): Flow<List<DailyGoalEntity>> = goals.observeRange(from.toEpochDay(), to.toEpochDay())
    fun observeAllGoals(): Flow<List<DailyGoalEntity>> = goals.observeAll()
    fun observeBooks(): Flow<List<BookEntity>> = books.observeAll()
    fun observeAchievements(): Flow<List<AchievementEntity>> = achievements.observeAll()
    fun observeBookProgress(): Flow<List<BookProgress>> = combine(books.observeAll(), sessions.observeBookPages()) { bs, pages ->
        val m = pages.associate { it.bookId to it.pages }
        bs.map { BookProgress(it, it.initialPage + (m[it.id] ?: 0)) }
    }

    private suspend fun validBookId(id: Long?): Long? = id?.let { books.get(it)?.id }

    /** Adds a session; the date comes from its start time and the configured daily reset time. */
    suspend fun addSession(i: SessionInput) {
        require(i.isValid) { "Enter pages, or a duration for a zero-page timed session" }
        val s = settings.settings.first()
        val now = System.currentTimeMillis()
        val t = i.startTimestamp ?: now
        val day = DayBoundary.logicalDate(Instant.ofEpochMilli(t), zone, s.resetMinutes)
        goals.insertIfAbsent(DailyGoalEntity(day.toEpochDay(), s.dailyGoal, s.resetMinutes)) // snapshot, never overwritten
        val bookId = validBookId(i.bookId)
        sessions.insert(ReadingSessionEntity(
            bookId = bookId, sessionDate = day.toEpochDay(), startTimestamp = t,
            endTimestamp = i.durationMinutes?.let { t + it * 60_000L },
            pagesRead = i.pages, startPage = i.startPage, endPage = i.endPage, durationMinutes = i.durationMinutes,
            notes = i.notes, createdAt = now, updatedAt = now))
        if (bookId != null) {
            val b = books.get(bookId)
            if (b != null && b.dateStarted == null) books.update(b.copy(dateStarted = t))
        }
    }

    suspend fun updateSession(old: ReadingSessionEntity, i: SessionInput) {
        require(i.isValid)
        val s = settings.settings.first()
        val t = i.startTimestamp ?: old.startTimestamp
        val day = DayBoundary.logicalDate(Instant.ofEpochMilli(t), zone, s.resetMinutes)
        goals.insertIfAbsent(DailyGoalEntity(day.toEpochDay(), s.dailyGoal, s.resetMinutes))
        sessions.update(old.copy(
            bookId = validBookId(i.bookId), sessionDate = day.toEpochDay(), startTimestamp = t,
            endTimestamp = i.durationMinutes?.let { t + it * 60_000L },
            pagesRead = i.pages, startPage = i.startPage, endPage = i.endPage, durationMinutes = i.durationMinutes,
            notes = i.notes, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(s: ReadingSessionEntity) = sessions.delete(s)

    suspend fun addBook(b: BookEntity) { books.insert(b) }
    suspend fun updateBook(b: BookEntity) = books.update(b)
    /** Sessions of a deleted book are kept (bookId becomes null) so history is never rewritten. */
    suspend fun deleteBook(b: BookEntity) = books.delete(b)

    suspend fun logicalToday(): LocalDate =
        DayBoundary.logicalDate(Instant.now(), zone, settings.settings.first().resetMinutes)

    /** Changes the default and today's snapshot only; earlier days keep their own goal. */
    suspend fun setDailyGoalNow(pages: Int) {
        settings.setDailyGoal(pages)
        goals.upsert(DailyGoalEntity(logicalToday().toEpochDay(), pages, settings.settings.first().resetMinutes))
    }

    /** Re-buckets every session into days using the new reset time. Missing goal snapshots get the current goal. */
    suspend fun setResetMinutes(minutes: Int) {
        settings.set(K.reset, minutes)
        val s = settings.settings.first()
        for (x in sessions.getAll()) {
            val day = DayBoundary.logicalDate(Instant.ofEpochMilli(x.startTimestamp), zone, minutes).toEpochDay()
            if (day != x.sessionDate) sessions.update(x.copy(sessionDate = day))
            goals.insertIfAbsent(DailyGoalEntity(day, s.dailyGoal, minutes))
        }
    }

    suspend fun resetStatistics() { sessions.deleteAll(); goals.deleteAll(); achievements.deleteAll() }

    suspend fun deleteAll() {
        sessions.deleteAll(); goals.deleteAll(); achievements.deleteAll(); reminders.deleteAll(); books.deleteAll()
    }
}
