package com.personal.readreminder

import com.personal.readreminder.domain.DayBoundary
import com.personal.readreminder.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val goal10 = { _: LocalDate -> 10 }

    @Test fun streakCountsBackFromToday() {
        val p = mapOf(today to 10, today.minusDays(1) to 12, today.minusDays(2) to 10)
        assertEquals(3, StreakCalculator.compute(today, p, goal10).current)
    }
    @Test fun incompleteTodayDoesNotBreakStreak() {
        val p = mapOf(today to 3, today.minusDays(1) to 10, today.minusDays(2) to 10)
        assertEquals(2, StreakCalculator.compute(today, p, goal10).current)
    }
    @Test fun missedYesterdayAndIncompleteTodayIsZero() {
        val p = mapOf(today to 3, today.minusDays(2) to 10)
        assertEquals(0, StreakCalculator.compute(today, p, goal10).current)
    }
    @Test fun longestStreakIsFound() {
        val p = (5..9).associate { today.minusDays(it.toLong()) to 10 } + (today to 10)
        val s = StreakCalculator.compute(today, p, goal10)
        assertEquals(1, s.current); assertEquals(5, s.longest); assertEquals(6, s.goalDays)
    }
    @Test fun historicalGoalSnapshotsAreRespected() {
        val y = today.minusDays(1)
        val goalFor = { d: LocalDate -> if (d == y) 5 else 20 }   // yesterday's goal was 5
        val p = mapOf(y to 6, today to 20)
        assertEquals(2, StreakCalculator.compute(today, p, goalFor).current)
    }
    @Test fun zeroGoalNeverCountsAsSuccess() {
        assertEquals(0, StreakCalculator.compute(today, mapOf(today to 50), { 0 }).current)
    }
    @Test fun sessionAt0130BelongsToPreviousDayWith0300Reset() {
        val zone = ZoneId.of("UTC")
        val t = Instant.parse("2026-10-19T01:30:00Z")
        assertEquals(LocalDate.of(2026, 10, 18), DayBoundary.logicalDate(t, zone, 180))
        assertEquals(LocalDate.of(2026, 10, 19), DayBoundary.logicalDate(t, zone, 0))
    }
}
