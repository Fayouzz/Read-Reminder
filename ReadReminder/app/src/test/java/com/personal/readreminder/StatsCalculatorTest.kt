package com.personal.readreminder

import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.domain.DayBoundary
import com.personal.readreminder.domain.DayState
import com.personal.readreminder.domain.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsCalculatorTest {
    private fun s(day: LocalDate, pages: Int, minutes: Int? = null) = ReadingSessionEntity(
        sessionDate = day.toEpochDay(), startTimestamp = 0, pagesRead = pages, durationMinutes = minutes, createdAt = 0, updatedAt = 0)

    @Test fun weekStripStartsOnChosenDay() {
        val wed = LocalDate.of(2026, 10, 21)
        assertEquals(LocalDate.of(2026, 10, 19), StatsCalculator.weekStrip(wed, true, emptyMap()) { 10 }.first().date)
        assertEquals(LocalDate.of(2026, 10, 18), StatsCalculator.weekStrip(wed, false, emptyMap()) { 10 }.first().date)
    }

    @Test fun weekStripStates() {
        val mon = LocalDate.of(2026, 10, 19)
        val strip = StatsCalculator.weekStrip(mon, true, mapOf(mon to 12, mon.plusDays(1) to 3)) { 10 }
        assertEquals(DayState.ACHIEVED, strip[0].state); assertEquals(DayState.PARTIAL, strip[1].state); assertEquals(DayState.NONE, strip[2].state)
    }

    @Test fun periodStatsOnlyCountElapsedDays() {
        val from = LocalDate.of(2026, 10, 1); val today = LocalDate.of(2026, 10, 10)
        val sessions = listOf(s(from, 10, 20), s(from, 5, 10), s(from.plusDays(1), 20))
        val st = StatsCalculator.periodStats(sessions, from, LocalDate.of(2026, 10, 31), today) { 10 }
        assertEquals(10, st.elapsedDays); assertEquals(35, st.totalPages); assertEquals(3, st.sessionCount)
        assertEquals(30, st.totalMinutes); assertEquals(15.0, st.avgSessionMinutes, 0.001)
        assertEquals(3.5, st.avgPagesPerDay, 0.001); assertEquals(17.5, st.avgPagesPerActiveDay, 0.001)
        assertEquals(2, st.goalDays); assertEquals(0.2, st.completionRate, 0.001)
    }

    @Test fun logicalDayRespectsResetTimeAndZone() {
        val ny = ZoneId.of("America/New_York")
        val t = Instant.parse("2026-03-08T06:30:00Z")   // 01:30 EST, the night clocks spring forward
        assertEquals(LocalDate.of(2026, 3, 8), DayBoundary.logicalDate(t, ny, 0))
        assertEquals(LocalDate.of(2026, 3, 7), DayBoundary.logicalDate(t, ny, 180))
        val fall = Instant.parse("2026-11-01T05:30:00Z")  // 01:30 EDT, clocks go back later that night
        assertEquals(LocalDate.of(2026, 11, 1), DayBoundary.logicalDate(fall, ny, 0))
    }

    @Test fun sessionCrossingMidnightBelongsToItsStartDay() {
        val utc = ZoneId.of("UTC")
        val start = Instant.parse("2026-10-19T23:50:00Z")      // 20 minute session ends after midnight
        assertEquals(LocalDate.of(2026, 10, 19), DayBoundary.logicalDate(start, utc, 0))
    }
}
