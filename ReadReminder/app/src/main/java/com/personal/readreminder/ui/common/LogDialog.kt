package com.personal.readreminder.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.readreminder.data.BookEntity
import com.personal.readreminder.data.BookStatus
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.data.SessionInput
import com.personal.readreminder.ui.theme.TextSecondary

/** Used for both "Log reading" (initial == null) and editing an existing session. */
@Composable
fun LogDialog(
    books: List<BookEntity>,
    initial: ReadingSessionEntity? = null,
    presetBookId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (SessionInput) -> Unit,
) {
    val selectable = books.filter { it.status == BookStatus.READING || it.id == initial?.bookId || it.id == presetBookId }
    var bookId by remember { mutableStateOf(if (initial != null) initial.bookId else (presetBookId ?: selectable.firstOrNull()?.id)) }
    var pagesText by remember { mutableStateOf(initial?.pagesRead?.toString() ?: "") }
    var durText by remember { mutableStateOf(initial?.durationMinutes?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var menuOpen by remember { mutableStateOf(false) }

    val pages = pagesText.toIntOrNull() ?: 0
    val dur = durText.toIntOrNull()
    val input = SessionInput(bookId, pages, dur, notes.trim().ifBlank { null })
    val showError = !input.isValid && (pagesText.isNotEmpty() || durText.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Log reading" else "Edit reading") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Book", color = TextSecondary)
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectable.firstOrNull { it.id == bookId }?.title ?: "No book (general reading)")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("No book (general reading)") }, onClick = { bookId = null; menuOpen = false })
                        selectable.forEach { b ->
                            DropdownMenuItem(text = { Text(b.title) }, onClick = { bookId = b.id; menuOpen = false })
                        }
                    }
                }
                Text("Pages read in this session", color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 20, 25).forEach { n ->
                        AssistChip(onClick = { pagesText = (pages + n).toString() }, label = { Text("+$n") })
                    }
                }
                OutlinedTextField(pagesText, { pagesText = it.filter(Char::isDigit).take(4) }, label = { Text("Pages") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(durText, { durText = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes (optional)") },
                    singleLine = true, isError = showError,
                    supportingText = { if (showError) Text("Enter pages, or minutes for a timed session with 0 pages") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(notes, { notes = it.take(300) }, label = { Text("Notes (optional)") })
            }
        },
        confirmButton = { TextButton(enabled = input.isValid, onClick = { onSave(input) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
