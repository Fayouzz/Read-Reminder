package com.personal.readreminder.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.readreminder.data.BookProgress
import com.personal.readreminder.data.BookStatus
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.data.SessionInput
import com.personal.readreminder.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Used for "Log reading" (initial == null) and for editing a session.
 * Two ways to enter progress: pages read directly, or start/stop page (pages read = stop - start).
 */
@Composable
fun LogDialog(
    books: List<BookProgress>,
    initial: ReadingSessionEntity? = null,
    presetBookId: Long? = null,
    initialTimestamp: Long? = null,
    defaultBookId: Long = -1L,
    is24h: Boolean = false,
    typicalMinutes: Int = 20,
    onDismiss: () -> Unit,
    onSave: (SessionInput) -> Unit,
) {
    val ctx = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val selectable = books.filter { it.book.status == BookStatus.READING || it.book.id == initial?.bookId || it.book.id == presetBookId }
    var bookId by remember {
        mutableStateOf<Long?>(
            if (initial != null) initial.bookId
            else presetBookId ?: defaultBookId.takeIf { id -> selectable.any { it.book.id == id } } ?: selectable.firstOrNull()?.book?.id
        )
    }
    var byRange by remember { mutableStateOf(initial?.startPage != null && initial.endPage != null) }
    var pagesText by remember { mutableStateOf(initial?.pagesRead?.toString() ?: "") }
    var startText by remember { mutableStateOf(initial?.startPage?.toString() ?: "") }
    var endText by remember { mutableStateOf(initial?.endPage?.toString() ?: "") }
    var durText by remember { mutableStateOf(initial?.durationMinutes?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var ts by remember { mutableStateOf(initial?.startTimestamp ?: initialTimestamp ?: System.currentTimeMillis()) }
    var menuOpen by remember { mutableStateOf(false) }

    val current = selectable.firstOrNull { it.book.id == bookId }
    val total = current?.book?.totalPages?.takeIf { it > 0 }
    val start = startText.toIntOrNull()
    val end = endText.toIntOrNull()
    val rangeError: String? = when {
        !byRange || startText.isEmpty() || endText.isEmpty() -> null
        start == null || end == null -> "Enter whole numbers"
        end < start -> "Stop page can't be before the start page"
        total != null && end > total -> "This book only has $total pages"
        else -> null
    }
    val rangeReady = !byRange || (start != null && end != null && rangeError == null)
    val pages = if (byRange) { if (start != null && end != null && end >= start) end - start else 0 } else (pagesText.toIntOrNull() ?: 0)
    val dur = durText.toIntOrNull()
    val input = SessionInput(
        bookId, pages, dur, notes.trim().ifBlank { null }, minOf(ts, System.currentTimeMillis()),
        if (byRange) start else null, if (byRange) end else null,
    )
    val valid = rangeReady && input.isValid
    val showError = !valid && rangeError == null && (pagesText.isNotEmpty() || durText.isNotEmpty() || (byRange && startText.isNotEmpty() && endText.isNotEmpty()))
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zone)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Log reading" else "Edit reading") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Book", color = TextSecondary)
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(current?.book?.title ?: "No book (general reading)")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("No book (general reading)") }, onClick = { bookId = null; menuOpen = false })
                        selectable.forEach { b ->
                            DropdownMenuItem(text = { Text(b.book.title) }, onClick = { bookId = b.book.id; menuOpen = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !byRange, onClick = { byRange = false }, label = { Text("Pages read") })
                    FilterChip(selected = byRange, onClick = {
                        byRange = true
                        if (startText.isEmpty()) startText = current?.currentPage?.toString() ?: ""
                    }, label = { Text("Start & stop page") })
                }
                if (!byRange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 25).forEach { n ->
                            AssistChip(onClick = { pagesText = (pages + n).toString() }, label = { Text("+$n") })
                        }
                    }
                    OutlinedTextField(pagesText, { pagesText = it.filter(Char::isDigit).take(4) }, label = { Text("Pages read in this session") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(startText, { startText = it.filter(Char::isDigit).take(5) }, label = { Text("Start page") },
                            singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(endText, { endText = it.filter(Char::isDigit).take(5) }, label = { Text("Stop page") },
                            singleLine = true, modifier = Modifier.weight(1f), isError = rangeError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Text(rangeError ?: "Pages read = stop page \u2212 start page ($pages). This records a session; the book's current page is worked out from your sessions, so nothing is counted twice.",
                        color = if (rangeError != null) MaterialTheme.colorScheme.error else TextSecondary)
                }
                OutlinedTextField(durText, { durText = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes (optional)") },
                    placeholder = { Text("e.g. $typicalMinutes") }, singleLine = true, isError = showError,
                    supportingText = { if (showError) Text("Enter pages, or minutes for a timed session with 0 pages") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("When", color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val dlg = DatePickerDialog(ctx, { _, y, m, d ->
                            ts = ZonedDateTime.of(LocalDate.of(y, m + 1, d), zdt.toLocalTime(), zone).toInstant().toEpochMilli()
                        }, zdt.year, zdt.monthValue - 1, zdt.dayOfMonth)
                        dlg.datePicker.maxDate = System.currentTimeMillis()
                        dlg.show()
                    }) { Text(zdt.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()))) }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        TimePickerDialog(ctx, { _, h, mi ->
                            ts = ZonedDateTime.of(zdt.toLocalDate(), LocalTime.of(h, mi), zone).toInstant().toEpochMilli()
                        }, zdt.hour, zdt.minute, is24h).show()
                    }) { Text(fmtMinutes(zdt.hour * 60 + zdt.minute, is24h)) }
                }
                OutlinedTextField(notes, { notes = it.take(300) }, label = { Text("Notes (optional)") })
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(input) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
