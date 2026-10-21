package com.personal.readreminder

import com.personal.readreminder.domain.AchStats
import com.personal.readreminder.domain.AchievementEvaluator
import com.personal.readreminder.domain.AchievementType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {
    @Test fun nothingUnlockedWithNoData() = assertTrue(AchievementEvaluator.satisfied(AchStats(0, 0, 0, 0, 0)).isEmpty())

    @Test fun firstSessionOnly() = assertEquals(setOf(FIRST_SESSION), AchievementEvaluator.satisfied(AchStats(5, 1, 0, 0, 0)))

    @Test fun streakTiers() {
        val r = AchievementEvaluator.satisfied(AchStats(200, 20, 8, 7, 0))
        assertTrue(STREAK_3 in r && STREAK_7 in r); assertTrue(STREAK_14 !in r && STREAK_30 !in r)
        assertTrue(FIRST_GOAL in r && GOAL_DAYS_7 in r && GOAL_DAYS_30 !in r)
    }

    @Test fun pageMilestonesAndBook() {
        val r = AchievementEvaluator.satisfied(AchStats(1000, 50, 30, 30, 1))
        assertTrue(listOf(PAGES_100, PAGES_500, PAGES_1000, FIRST_BOOK, STREAK_30, GOAL_DAYS_30).all { it in r })
    }
}
