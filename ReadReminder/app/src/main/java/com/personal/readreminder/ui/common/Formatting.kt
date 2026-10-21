package com.personal.readreminder.ui.common

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun fmtMinutes(minutesOfDay: Int, is24h: Boolean): String =
    LocalTime.of((minutesOfDay / 60) % 24, minutesOfDay % 60)
        .format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault()))

fun fmtInstant(epochMillis: Long, is24h: Boolean): String {
    val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return fmtMinutes(t.hour * 60 + t.minute, is24h)
}

fun fmtDateTime(epochMillis: Long, is24h: Boolean): String {
    val z = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val d = z.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    return d + ", " + fmtMinutes(z.hour * 60 + z.minute, is24h)
}

fun fmtDuration(totalMinutes: Int): String =
    if (totalMinutes < 60) "$totalMinutes min" else "${totalMinutes / 60} h ${totalMinutes % 60} min"
