package com.personal.readreminder.domain

import com.personal.readreminder.data.BookEntity
import com.personal.readreminder.data.BookStatus
import com.personal.readreminder.data.DailyGoalEntity
import com.personal.readreminder.data.ReadingSessionEntity
import java.time.LocalDate

enum class AchievementType(val title: String, val description: String, val emoji: String) {
    FIRST_SESSION("First Reading Session", "Log your first reading session", "\uD83D\uDCD6"),
    FIRST_GOAL("First Daily Goal", "Reach your daily page goal once", "\uD83C\uDFAF"),
    STREAK_3("3-Day Streak", "Reach your goal 3 days in a row", "\uD83D\uDD25"),
    STREAK_7("7-Day Streak", "Reach your goal 7 days in a row", "\uD83D\uDD25"),
    STREAK_14("14-Day Streak", "Reach your goal 14 days in a row", "\uD83D\uDD25"),
    STREAK_30("30-Day Streak", "Reach your goal 30 days in a row", "\uD83D\uDD25"),
    PAGES_100("100 Pages Read", "Read 100 pages in total", "\uD83D\uDCC4"),
    PAGES_500("500 Pages Read", "Read 500 pages in total", "\uD83D\uDCDA"),
    PAGES_1000("1,000 Pages Read", "Read 1,000 pages in total", "\uD83C\uDFC6"),
    FIRST_BOOK("First Book Completed", "Mark a book as completed", "\u2705"),
    GOAL_DAYS_7("7 Goal Days", "Reach your goal on 7 different days", "\u2B50"),
    GOAL_DAYS_30("30 Goal Days", "Reach your goal on 30 different days", "\uD83C\uDF1F"),
}

data class AchStats(val totalPages: Int, val sessionCount: Int, val goalDays: Int, val longestStreak: Int, val booksCompleted: Int)

object AchievementEvaluator {
    fun stats(sessions: List<ReadingSessionEntity>, goals: List<DailyGoalEntity>, books: List<BookEntity>, fallbackGoal: Int, today: LocalDate): AchStats {
        val byDay = StatsCalculator.pagesByDay(sessions)
        val snap = goals.associate { LocalDate.ofEpochDay(it.date) to it.targetPages }
        val st = StreakCalculator.compute(today, byDay) { snap[it] ?: fallbackGoal }
        return AchStats(sessions.sumOf { it.pagesRead }, sessions.size, st.goalDays, st.longest, books.count { it.status == BookStatus.COMPLETED })
    }

    fun satisfied(s: AchStats): Set<AchievementType> = buildSet {
        if (s.sessionCount >= 1) add(AchievementType.FIRST_SESSION)
        if (s.goalDays >= 1) add(AchievementType.FIRST_GOAL)
        if (s.longestStreak >= 3) add(AchievementType.STREAK_3)
        if (s.longestStreak >= 7) add(AchievementType.STREAK_7)
        if (s.longestStreak >= 14) add(AchievementType.STREAK_14)
        if (s.longestStreak >= 30) add(AchievementType.STREAK_30)
        if (s.totalPages >= 100) add(AchievementType.PAGES_100)
        if (s.totalPages >= 500) add(AchievementType.PAGES_500)
        if (s.totalPages >= 1000) add(AchievementType.PAGES_1000)
        if (s.booksCompleted >= 1) add(AchievementType.FIRST_BOOK)
        if (s.goalDays >= 7) add(AchievementType.GOAL_DAYS_7)
        if (s.goalDays >= 30) add(AchievementType.GOAL_DAYS_30)
    }
}
