package com.personal.readreminder.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.BookStatus
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.ui.common.*
import com.personal.readreminder.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    vm: HomeViewModel, onOpenBooks: () -> Unit, onOpenGoal: () -> Unit, onOpenAchievements: () -> Unit, onOpenReminders: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cfg = s.cfg
    var showLog by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ReadingSessionEntity?>(null) }
    var editing by remember { mutableStateOf<ReadingSessionEntity?>(null) }
    var changeBook by remember { mutableStateOf(false) }
    var logBook by remember { mutableStateOf<Long?>(null) }
    var celebrate by remember { mutableStateOf(false) }
    var wasReached by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(s.loaded, s.goalReached) {
        if (s.loaded) { if (wasReached == false && s.goalReached) celebrate = true; wasReached = s.goalReached }
    }
    val hour = LocalTime.now().hour
    val greet = when { hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" } +
        (if (cfg.displayName.isNotBlank()) ", ${cfg.displayName}!" else "!")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PrimaryBlue, BackgroundBlue)))) {
                Canvas(Modifier.matchParentSize()) {   // soft decorative bubbles
                    drawCircle(Color.White, 34.dp.toPx(), Offset(size.width * 0.9f, size.height * 0.2f), alpha = 0.12f)
                    drawCircle(Color.White, 18.dp.toPx(), Offset(size.width * 0.75f, size.height * 0.85f), alpha = 0.15f)
                    drawCircle(Color.White, 10.dp.toPx(), Offset(size.width * 0.05f, size.height * 0.9f), alpha = 0.15f)
                }
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(Modifier.size(72.dp))
                    Spacer(Modifier.width(12.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, contentColor = TextMain, shadowElevation = 4.dp) {
                        Column(Modifier.padding(12.dp)) {
                            Text(greet, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(s.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(s.message, fontSize = 14.sp)
                            if (cfg.targetMessage.isNotBlank()) Text(cfg.targetMessage, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(s.progress, s.pagesToday, s.goal, if (cfg.dashboardMinutes) s.minutesToday else null)
                Spacer(Modifier.height(8.dp))
                Text(if (s.goalReached) "Daily goal achieved \u2713" else "${s.percent}% completed",
                    color = if (s.goalReached) Success else TextSecondary, fontWeight = FontWeight.SemiBold)
                Text("Current streak: ${s.currentStreak} day${if (s.currentStreak == 1) "" else "s"}  \u00B7  Longest: ${s.longestStreak}",
                    color = TextSecondary, fontSize = 13.sp)
                if (s.nextReminderAt > 0L) {
                    Row(Modifier.clickable(onClick = onOpenReminders).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Next reminder: " + fmtNext(s.nextReminderAt, cfg.time24h), fontSize = 13.sp)
                    }
                } else if (!cfg.remindersEnabled) {
                    Text("Reminders are off", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onOpenReminders).padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showLog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(26.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Log Reading", fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenBooks, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Books", maxLines = 1) }
                    OutlinedButton(onClick = onOpenGoal, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Daily goal", maxLines = 1) }
                    OutlinedButton(onClick = onOpenAchievements, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Badges", maxLines = 1) }
                }
            }
        }
        item {
            val b = s.currentBook
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    if (b == null) {
                        Text("No book selected", fontWeight = FontWeight.SemiBold)
                        Text("Add a book to track pages per book.", color = TextSecondary, fontSize = 13.sp)
                        TextButton(onClick = onOpenBooks) { Text("Add a book") }
                    } else {
                        Text("Currently reading", color = TextSecondary, fontSize = 12.sp)
                        Text(b.book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val total = b.book.totalPages
                        Text(if (total != null && total > 0) "Page ${b.currentPage} of $total" + (b.fraction?.let { " \u00B7 ${(it * 100).toInt()}%" } ?: "") else "Page ${b.currentPage}",
                            fontSize = 13.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { logBook = b.book.id }) { Text("Log pages") }
                            if (s.books.count { it.book.status == BookStatus.READING } > 1) TextButton(onClick = { changeBook = true }) { Text("Change book") }
                        }
                    }
                }
            }
        }
        item { Text("Today's records", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (s.sessions.isEmpty()) {
            item { Text("No reading logged yet today. Tap \u201CLog Reading\u201D after your next few pages.", Modifier.padding(16.dp), color = TextSecondary) }
        }
        items(s.sessions, key = { it.id }) { r ->
            RecordRow(r, s.books.firstOrNull { it.book.id == r.bookId }?.book?.title, cfg.time24h, cfg.compactRecords,
                onEdit = { editing = r }, onDelete = { toDelete = r })
        }
    }

    if (showLog) LogDialog(books = s.books, defaultBookId = cfg.defaultBookId, is24h = cfg.time24h, typicalMinutes = cfg.defaultSessionMinutes,
        onDismiss = { showLog = false }, onSave = { vm.log(it); showLog = false })
    logBook?.let { id -> LogDialog(books = s.books, presetBookId = id, is24h = cfg.time24h, typicalMinutes = cfg.defaultSessionMinutes,
        onDismiss = { logBook = null }, onSave = { vm.log(it); logBook = null }) }
    editing?.let { r -> LogDialog(books = s.books, initial = r, is24h = cfg.time24h, typicalMinutes = cfg.defaultSessionMinutes,
        onDismiss = { editing = null }, onSave = { vm.edit(r, it); editing = null }) }
    if (changeBook) {
        ChoiceDialog("Change book", s.books.filter { it.book.status == BookStatus.READING }.map { it.book.id to it.book.title },
            s.currentBook?.book?.id ?: -1L, onSelect = { vm.setDefaultBook(it); changeBook = false }, onDismiss = { changeBook = false })
    }
    toDelete?.let { r ->
        ConfirmDialog("Delete this record?", "${r.pagesRead} pages will be removed and your totals and streak recalculated.", "Delete",
            onConfirm = { vm.delete(r); toDelete = null }, onDismiss = { toDelete = null })
    }
    if (celebrate) Celebration("\uD83C\uDF89", "Daily goal reached!", "Amazing! You reached today's reading goal.") { celebrate = false }
}

private fun fmtNext(epochMillis: Long, is24h: Boolean): String {
    val z = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now()
    val day = when (z.toLocalDate()) { today -> "today"; today.plusDays(1) -> "tomorrow"; else -> z.format(DateTimeFormatter.ofPattern("EEE d MMM")) }
    return fmtInstant(epochMillis, is24h) + " " + day
}

@Composable
fun ProgressRing(progress: Float, pages: Int, goal: Int, minutes: Int? = null) {
    val anim by animateFloatAsState(progress, tween(600), label = "ring")
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 22.dp.toPx(); val inset = w / 2
            val sz = Size(size.width - w, size.height - w)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
            if (anim > 0f) drawArc(PrimaryBlue, -90f, 360f * anim, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (minutes != null) {
                Text("$minutes min", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("$pages / $goal pages", color = TextSecondary)
            } else {
                Text("$pages / $goal", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("pages today", color = TextSecondary)
            }
        }
    }
}

/** Original mascot: a friendly blue blob holding a little book. */
@Composable
fun Mascot(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color.White, radius = w * 0.46f, center = Offset(w / 2, h * 0.48f), alpha = 0.95f)
        drawCircle(LightBlue, radius = w * 0.40f, center = Offset(w / 2, h * 0.48f))
        listOf(0.38f, 0.62f).forEach { x ->
            drawCircle(Color.White, w * 0.09f, Offset(w * x, h * 0.40f))
            drawCircle(TextMain, w * 0.045f, Offset(w * x, h * 0.41f))
        }
        drawArc(TextMain, 20f, 140f, false, Offset(w * 0.38f, h * 0.44f), Size(w * 0.24f, h * 0.16f), style = Stroke(w * 0.025f, cap = StrokeCap.Round))
        drawRect(Color.White, Offset(w * 0.30f, h * 0.68f), Size(w * 0.18f, h * 0.20f))
        drawRect(Color(0xFFE6F3FD), Offset(w * 0.50f, h * 0.68f), Size(w * 0.18f, h * 0.20f))
        drawRect(Success, Offset(w * 0.48f, h * 0.68f), Size(w * 0.02f, h * 0.20f))
    }
}
