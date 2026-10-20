package com.personal.readreminder.data

import com.personal.readreminder.domain.DayBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the user typed in the log dialog. pages may be 0 only for a timed session (durationMinutes > 0). */
data class SessionInput(val bookId: Long?, val pages: Int, val durationMinutes: Int?, val notes: String?) {
    val isValid: Boolean get() = pages >= 0 && (pages > 0 || (durationMinutes ?: 0) > 0)
}

class ReadingRepository(
    private val sessions: SessionDao,
    private val goals: GoalDao,
    private val books: BookDao,
    private val settings: SettingsRepository,
) {
    fun observeSessions(day: LocalDate): Flow<List<ReadingSessionEntity>> = sessions.observeDay(day.toEpochDay())
    fun observeTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = sessions.observeTotals(from.toEpochDay(), to.toEpochDay())
    fun observeGoals(from: LocalDate, to: LocalDate): Flow<List<DailyGoalEntity>> = goals.observeRange(from.toEpochDay(), to.toEpochDay())
    fun observeBooks(): Flow<List<BookEntity>> = books.observeAll()
    fun observeBookPages(): Flow<List<BookPages>> = sessions.observeBookPages()

    suspend fun logNow(i: SessionInput) {
        val reset = settings.settings.first().resetMinutes
        val now = Instant.now()
        logPages(i.pages, DayBoundary.logicalDate(now, ZoneId.systemDefault(), reset), now, i.bookId, i.notes, i.durationMinutes)
    }

    suspend fun logPages(pages: Int, day: LocalDate, now: Instant, bookId: Long? = null, notes: String? = null, durationMinutes: Int? = null) {
        require(pages >= 0 && (pages > 0 || (durationMinutes ?: 0) > 0)) { "Enter pages, or a duration for a zero-page timed session" }
        val s = settings.settings.first()
        goals.insertIfAbsent(DailyGoalEntity(day.toEpochDay(), s.dailyGoal, s.resetMinutes)) // snapshot, never overwritten
        val t = now.toEpochMilli()
        sessions.insert(ReadingSessionEntity(bookId = bookId, sessionDate = day.toEpochDay(), startTimestamp = t,
            pagesRead = pages, durationMinutes = durationMinutes, notes = notes, createdAt = t, updatedAt = t))
        if (bookId != null) {
            val b = books.get(bookId)
            if (b != null && b.dateStarted == null) books.update(b.copy(dateStarted = t))
        }
    }

    /** Editing keeps the original date/time so history stays where it was. */
    suspend fun updateSession(old: ReadingSessionEntity, i: SessionInput) {
        require(i.isValid)
        sessions.update(old.copy(bookId = i.bookId, pagesRead = i.pages, durationMinutes = i.durationMinutes,
            notes = i.notes, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(s: ReadingSessionEntity) = sessions.delete(s)

    suspend fun addBook(b: BookEntity) { books.insert(b) }
    suspend fun updateBook(b: BookEntity) = books.update(b)
    /** Sessions of a deleted book are kept (bookId becomes null) so history is never rewritten. */
    suspend fun deleteBook(b: BookEntity) = books.delete(b)

    /** Changes the default and today's snapshot only; earlier days keep their own goal. */
    suspend fun setDailyGoal(pages: Int, today: LocalDate) {
        settings.setDailyGoal(pages)
        goals.upsert(DailyGoalEntity(today.toEpochDay(), pages, settings.settings.first().resetMinutes))
    }
}
