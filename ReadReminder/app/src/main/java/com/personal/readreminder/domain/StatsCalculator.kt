package com.personal.readreminder.domain

import com.personal.readreminder.data.ReadingSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class DayState { ACHIEVED, PARTIAL, NONE }

data class DayStatus(val date: LocalDate, val pages: Int, val goal: Int) {
    val state: DayState get() = when {
        goal > 0 && pages >= goal -> DayState.ACHIEVED
        pages > 0 -> DayState.PARTIAL
        else -> DayState.NONE
    }
}

data class PeriodStats(
    val elapsedDays: Int = 0, val totalPages: Int = 0, val sessionCount: Int = 0, val totalMinutes: Int = 0,
    val avgPagesPerDay: Double = 0.0, val avgPagesPerActiveDay: Double = 0.0, val avgSessionMinutes: Double = 0.0,
    val goalDays: Int = 0, val completionRate: Double = 0.0, val activeDays: Int = 0,
)

object StatsCalculator {
    fun pagesByDay(sessions: List<ReadingSessionEntity>): Map<LocalDate, Int> =
        sessions.groupBy { LocalDate.ofEpochDay(it.sessionDate) }.mapValues { e -> e.value.sumOf { it.pagesRead } }

    fun weekStrip(anchor: LocalDate, mondayStart: Boolean, pagesByDay: Map<LocalDate, Int>, goalFor: (LocalDate) -> Int): List<DayStatus> {
        val first = if (mondayStart) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val start = anchor.with(TemporalAdjusters.previousOrSame(first))
        return (0..6).map { start.plusDays(it.toLong()) }.map { DayStatus(it, pagesByDay[it] ?: 0, goalFor(it)) }
    }

    /** Statistics for [from]..[to]; days after [today] are not counted as elapsed. */
    fun periodStats(sessions: List<ReadingSessionEntity>, from: LocalDate, to: LocalDate, today: LocalDate, goalFor: (LocalDate) -> Int): PeriodStats {
        val end = if (to.isAfter(today)) today else to
        if (end.isBefore(from)) return PeriodStats()
        val elapsed = ChronoUnit.DAYS.between(from, end).toInt() + 1
        val inRange = sessions.filter { val d = LocalDate.ofEpochDay(it.sessionDate); !d.isBefore(from) && !d.isAfter(end) }
        val byDay = pagesByDay(inRange)
        val total = inRange.sumOf { it.pagesRead }
        val minutes = inRange.sumOf { it.durationMinutes ?: 0 }
        val timed = inRange.filter { (it.durationMinutes ?: 0) > 0 }
        var goalDays = 0
        var d = from
        while (!d.isAfter(end)) { val g = goalFor(d); if (g > 0 && (byDay[d] ?: 0) >= g) goalDays++; d = d.plusDays(1) }
        val active = byDay.values.count { it > 0 }
        return PeriodStats(
            elapsedDays = elapsed, totalPages = total, sessionCount = inRange.size, totalMinutes = minutes,
            avgPagesPerDay = total.toDouble() / elapsed,
            avgPagesPerActiveDay = if (active > 0) total.toDouble() / active else 0.0,
            avgSessionMinutes = if (timed.isNotEmpty()) timed.sumOf { it.durationMinutes ?: 0 }.toDouble() / timed.size else 0.0,
            goalDays = goalDays, completionRate = goalDays.toDouble() / elapsed, activeDays = active,
        )
    }
}
