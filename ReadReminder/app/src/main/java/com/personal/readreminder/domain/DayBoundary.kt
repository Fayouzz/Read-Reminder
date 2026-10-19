package com.personal.readreminder.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DayBoundary {
    /** Logical reading date: the day starts at [resetMinutes] after local midnight (0 = midnight). */
    fun logicalDate(instant: Instant, zone: ZoneId, resetMinutes: Int): LocalDate =
        instant.atZone(zone).minusMinutes(resetMinutes.toLong()).toLocalDate()
}
