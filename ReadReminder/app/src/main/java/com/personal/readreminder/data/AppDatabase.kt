package com.personal.readreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, ReadingSessionEntity::class, DailyGoalEntity::class, ReminderEntity::class, AchievementEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun goalDao(): GoalDao
    abstract fun reminderDao(): ReminderDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        // Future schema changes must add a Migration here; no destructive fallback on purpose.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "read_reminder.db").build()
    }
}
