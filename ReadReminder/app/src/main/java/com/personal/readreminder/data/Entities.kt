package com.personal.readreminder.data

import androidx.room.*

enum class BookStatus { READING, COMPLETED, ARCHIVED }

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String? = null,
    val totalPages: Int? = null,
    val initialPage: Int = 0,
    val status: BookStatus = BookStatus.READING,
    val coverUri: String? = null,
    val coverColor: Int? = null,
    val dateAdded: Long,
    val dateStarted: Long? = null,
    val dateCompleted: Long? = null,
    val notes: String? = null,
)

/** Source of truth for all statistics. sessionDate = logical epoch-day (after applying the daily reset time). */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [ForeignKey(BookEntity::class, ["id"], ["bookId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("bookId"), Index("sessionDate")],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long? = null,
    val sessionDate: Long,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val pagesRead: Int,
    val startPage: Int? = null,
    val endPage: Int? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One snapshot row per day so changing the goal never rewrites past performance. */
@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val date: Long,
    val targetPages: Int,
    val resetMinutes: Int = 0,
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val weekdayMask: Int = 0b1111111, // bit0 = Monday ... bit6 = Sunday
    val message: String? = null,
    val mode: String = "FIXED",
    val intervalMinutes: Int? = null,
    val targetPages: Int? = null,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementType: String,
    val unlockedAt: Long,
)
