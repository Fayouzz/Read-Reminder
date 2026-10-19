package com.personal.readreminder.domain

import java.time.LocalDate

data class Streaks(val current: Int, val longest: Int, val goalDays: Int)

object StreakCalculator {
    /**
     * A day is successful when pages >= that day's goal (goal > 0).
     * Current streak = consecutive successful days ending today, or yesterday if today is not yet complete.
     */
    fun compute(today: LocalDate, pagesByDay: Map<LocalDate, Int>, goalFor: (LocalDate) -> Int): Streaks {
        fun ok(d: LocalDate): Boolean { val g = goalFor(d); return g > 0 && (pagesByDay[d] ?: 0) >= g }

        var cursor = if (ok(today)) today else today.minusDays(1)
        var current = 0
        while (ok(cursor)) { current++; cursor = cursor.minusDays(1) }

        val days = pagesByDay.keys.filter { ok(it) }.sorted()
        var longest = 0; var run = 0; var prev: LocalDate? = null
        for (d in days) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            if (run > longest) longest = run
            prev = d
        }
        return Streaks(current, longest, days.size)
    }
}
