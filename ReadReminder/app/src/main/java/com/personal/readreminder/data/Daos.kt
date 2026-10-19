package com.personal.readreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class DayTotal(val day: Long, val pages: Int)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC") fun observeAll(): Flow<List<BookEntity>>
    @Insert suspend fun insert(book: BookEntity): Long
    @Update suspend fun update(book: BookEntity)
    @Delete suspend fun delete(book: BookEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM reading_sessions WHERE sessionDate = :day ORDER BY startTimestamp DESC")
    fun observeDay(day: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT sessionDate AS day, SUM(pagesRead) AS pages FROM reading_sessions WHERE sessionDate BETWEEN :from AND :to GROUP BY sessionDate")
    fun observeTotals(from: Long, to: Long): Flow<List<DayTotal>>

    @Insert suspend fun insert(s: ReadingSessionEntity): Long
    @Update suspend fun update(s: ReadingSessionEntity)
    @Delete suspend fun delete(s: ReadingSessionEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM daily_goals WHERE date BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<DailyGoalEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIfAbsent(g: DailyGoalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(g: DailyGoalEntity)
}
