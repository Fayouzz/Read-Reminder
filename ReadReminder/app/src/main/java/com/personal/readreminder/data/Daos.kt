package com.personal.readreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class DayTotal(val day: Long, val pages: Int)
data class BookPages(val bookId: Long, val pages: Int)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC") fun observeAll(): Flow<List<BookEntity>>
    @Query("SELECT * FROM books ORDER BY id") suspend fun getAll(): List<BookEntity>
    @Query("SELECT * FROM books WHERE id = :id") suspend fun get(id: Long): BookEntity?
    @Insert suspend fun insert(book: BookEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(books: List<BookEntity>)
    @Update suspend fun update(book: BookEntity)
    @Delete suspend fun delete(book: BookEntity)
    @Query("DELETE FROM books") suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY startTimestamp DESC")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTimestamp")
    suspend fun getAll(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE sessionDate = :day ORDER BY startTimestamp DESC")
    fun observeDay(day: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT sessionDate AS day, SUM(pagesRead) AS pages FROM reading_sessions WHERE sessionDate BETWEEN :from AND :to GROUP BY sessionDate")
    fun observeTotals(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("SELECT bookId AS bookId, SUM(pagesRead) AS pages FROM reading_sessions WHERE bookId IS NOT NULL GROUP BY bookId")
    fun observeBookPages(): Flow<List<BookPages>>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions WHERE sessionDate = :day")
    suspend fun pagesOnDay(day: Long): Int

    @Query("SELECT MAX(createdAt) FROM reading_sessions")
    suspend fun lastCreatedAt(): Long?

    @Insert suspend fun insert(s: ReadingSessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<ReadingSessionEntity>)
    @Update suspend fun update(s: ReadingSessionEntity)
    @Delete suspend fun delete(s: ReadingSessionEntity)
    @Query("DELETE FROM reading_sessions") suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM daily_goals WHERE date BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<DailyGoalEntity>>
    @Query("SELECT * FROM daily_goals") fun observeAll(): Flow<List<DailyGoalEntity>>
    @Query("SELECT * FROM daily_goals") suspend fun getAll(): List<DailyGoalEntity>
    @Query("SELECT * FROM daily_goals WHERE date = :date") suspend fun get(date: Long): DailyGoalEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfAbsent(g: DailyGoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(g: DailyGoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<DailyGoalEntity>)
    @Query("DELETE FROM daily_goals") suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour, minute, id") fun observeAll(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders ORDER BY hour, minute, id") suspend fun getAllOnce(): List<ReminderEntity>
    @Insert suspend fun insert(r: ReminderEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<ReminderEntity>)
    @Update suspend fun update(r: ReminderEntity)
    @Delete suspend fun delete(r: ReminderEntity)
    @Query("DELETE FROM reminders") suspend fun deleteAll()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements") fun observeAll(): Flow<List<AchievementEntity>>
    @Query("SELECT * FROM achievements") suspend fun getAll(): List<AchievementEntity>
    /** Returns -1 when the achievement was already unlocked, so no duplicate unlock is ever announced. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfAbsent(a: AchievementEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<AchievementEntity>)
    @Query("DELETE FROM achievements") suspend fun deleteAll()
}
