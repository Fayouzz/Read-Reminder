package com.personal.readreminder.ui.books

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.BookEntity
import com.personal.readreminder.data.BookProgress
import com.personal.readreminder.data.BookStatus
import com.personal.readreminder.ui.common.LogDialog
import com.personal.readreminder.ui.theme.*

private val Swatches = listOf(0xFF55AEF5, 0xFF35B86B, 0xFFFFB547, 0xFFE57373, 0xFF9575CD, 0xFF4DB6AC).map { it.toInt() }

@Composable
fun BooksScreen(vm: BooksViewModel, onBack: () -> Unit) {
    val list by vm.books.collectAsStateWithLifecycle()
    val cfg by vm.cfg.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BookEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var logFor by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<BookEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("My books", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add a book")
                }
            }
            if (list.isEmpty()) {
                item { Text("No books yet. Add the book you're reading, then log pages against it.", color = TextSecondary) }
            }
            for ((status, title) in listOf(BookStatus.READING to "Currently reading", BookStatus.COMPLETED to "Completed", BookStatus.ARCHIVED to "Archived")) {
                val group = list.filter { it.book.status == status }
                if (group.isNotEmpty()) {
                    item(key = "header_$status") { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp)) }
                    items(group, key = { it.book.id }) { ui ->
                        BookCard(ui,
                            onLog = { logFor = ui.book.id },
                            onEdit = { editing = ui.book },
                            onStatus = { vm.setStatus(ui.book, it) },
                            onDelete = { deleting = ui.book })
                    }
                }
            }
        }
    }

    if (adding) BookDialog(null, { adding = false }) { vm.save(it); adding = false }
    editing?.let { b -> BookDialog(b, { editing = null }) { vm.save(it); editing = null } }
    logFor?.let { id -> LogDialog(books = list, presetBookId = id, is24h = cfg.time24h, typicalMinutes = cfg.defaultSessionMinutes, onDismiss = { logFor = null }, onSave = { vm.log(it); logFor = null }) }
    deleting?.let { b ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text("Delete \u201C${b.title}\u201D?") },
            text = { Text("The book is removed. Your logged reading sessions stay in your history and totals.") },
            confirmButton = { TextButton(onClick = { vm.delete(b); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
private fun BookCard(ui: BookProgress, onLog: () -> Unit, onEdit: () -> Unit, onStatus: (BookStatus) -> Unit, onDelete: () -> Unit) {
    val b = ui.book
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(10.dp).fillMaxHeight().background(Color(b.coverColor ?: Swatches[0])))
            Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (!b.author.isNullOrBlank()) Text(b.author, color = TextSecondary, fontSize = 13.sp)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Book options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                            if (b.status != BookStatus.READING) DropdownMenuItem(text = { Text("Move to currently reading") }, onClick = { menu = false; onStatus(BookStatus.READING) })
                            if (b.status != BookStatus.COMPLETED) DropdownMenuItem(text = { Text("Mark as completed") }, onClick = { menu = false; onStatus(BookStatus.COMPLETED) })
                            if (b.status != BookStatus.ARCHIVED) DropdownMenuItem(text = { Text("Archive") }, onClick = { menu = false; onStatus(BookStatus.ARCHIVED) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                        }
                    }
                }
                val total = b.totalPages
                Text(if (total != null && total > 0) "Page ${ui.currentPage} of $total" else "Page ${ui.currentPage}", fontSize = 14.sp)
                ui.fraction?.let { f ->
                    LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = PrimaryBlue, trackColor = Divider)
                    Text("${(f * 100).toInt()}% completed", color = TextSecondary, fontSize = 12.sp)
                }
                if (ui.reachedEnd && b.status == BookStatus.READING) {
                    Text("You've reached the last page. Finished?", color = Success, fontSize = 13.sp)
                    TextButton(onClick = { onStatus(BookStatus.COMPLETED) }) { Text("Mark as completed") }
                }
                if (b.status == BookStatus.READING) {
                    OutlinedButton(onClick = onLog, shape = RoundedCornerShape(20.dp)) { Text("Log pages") }
                }
            }
        }
    }
}

@Composable
private fun BookDialog(initial: BookEntity?, onDismiss: () -> Unit, onSave: (BookEntity) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var author by remember { mutableStateOf(initial?.author ?: "") }
    var totalText by remember { mutableStateOf(initial?.totalPages?.toString() ?: "") }
    var currentText by remember { mutableStateOf(initial?.initialPage?.takeIf { it > 0 }?.toString() ?: "") }
    var color by remember { mutableStateOf(initial?.coverColor ?: Swatches[0]) }

    val total = totalText.toIntOrNull()?.takeIf { it > 0 }
    val start = currentText.toIntOrNull() ?: 0
    val pageError = total != null && start > total
    val valid = title.isNotBlank() && !pageError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add a book" else "Edit book") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(author, { author = it.take(80) }, label = { Text("Author (optional)") }, singleLine = true)
                OutlinedTextField(totalText, { totalText = it.filter(Char::isDigit).take(5) }, label = { Text("Total pages (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(currentText, { currentText = it.filter(Char::isDigit).take(5) }, label = { Text("Page you're starting from (optional)") }, singleLine = true,
                    isError = pageError, supportingText = { Text(if (pageError) "Can't be more than the total pages" else "Pages you log are added on top of this") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Cover color", color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Swatches.forEach { c ->
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable(onClickLabel = "Choose cover color") { color = c })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val now = System.currentTimeMillis()
                onSave(BookEntity(
                    id = initial?.id ?: 0, title = title.trim(), author = author.trim().ifBlank { null },
                    totalPages = total, initialPage = start, status = initial?.status ?: BookStatus.READING,
                    coverUri = initial?.coverUri, coverColor = color, dateAdded = initial?.dateAdded ?: now,
                    dateStarted = initial?.dateStarted, dateCompleted = initial?.dateCompleted, notes = initial?.notes,
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
