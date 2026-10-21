package com.personal.readreminder.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.ui.common.*
import com.personal.readreminder.ui.theme.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(vm: HistoryViewModel) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val s = ui
    if (s == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ReadingSessionEntity?>(null) }
    var deleting by remember { mutableStateOf<ReadingSessionEntity?>(null) }
    val month = s.mode == HistoryMode.MONTH
    val cfg = s.cfg
    val title = if (month) s.ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + s.ym.year else s.year.toString()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = month, onClick = { vm.setMode(HistoryMode.MONTH) }, label = { Text("Month") })
            FilterChip(selected = !month, onClick = { vm.setMode(HistoryMode.YEAR) }, label = { Text("Year") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous") }
            Text(title, fontWeight = FontWeight.Bold)
            IconButton(onClick = { vm.move(1) }, enabled = s.canGoNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
        }

        if (!s.hasAnyData) {
            Text("No reading logged yet. Log your first session on the Home tab and your charts and statistics will appear here.",
                Modifier.padding(16.dp), color = TextSecondary)
        }

        val labels = remember { (1..12).map { java.time.Month.of(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) } }
        BarChart(
            values = s.bars, selected = s.selectedBar, goal = s.goalLine,
            xLabel = { if (month) (it + 1).toString() else labels[it] }, labelEvery = if (month) 5 else 1,
            onSelect = vm::selectBar,
            description = (if (month) "Pages read per day in $title" else "Pages read per month in $title") + ". Tap a bar to see that " + (if (month) "day." else "month."),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        val summary = if (month)
            s.selectedDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + ": " + s.selectedPages + " pages (goal " + s.selectedGoal + ")" + (if (s.selectedPages >= s.selectedGoal && s.selectedGoal > 0) " \u2713" else "")
        else if (s.selectedBar >= 0)
            s.ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ": " + s.bars[s.selectedBar] + " pages \u00B7 " + s.goalDaysPerMonth[s.selectedBar] + " goal days"
        else "Tap a month to see details"
        Text(summary, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontWeight = FontWeight.SemiBold)
        if (month) Text("Dashed line = daily goal; green bars reached it.", Modifier.padding(horizontal = 16.dp), color = TextSecondary, fontSize = 12.sp)

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(top = 12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Week", fontWeight = FontWeight.Bold)
                WeekStrip(s.week)
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text(if (month) "Statistics \u2013 $title" else "Statistics \u2013 ${s.year}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            val st = s.stats
            StatRow("Average pages per day", "%.1f".format(st.avgPagesPerDay))
            StatRow("Average on reading days", "%.1f".format(st.avgPagesPerActiveDay))
            StatRow("Total pages read", st.totalPages.toString())
            StatRow("Reading sessions", st.sessionCount.toString())
            StatRow("Total reading time", if (st.totalMinutes > 0) fmtDuration(st.totalMinutes) else "\u2013")
            StatRow("Average session length", if (st.avgSessionMinutes > 0) fmtDuration(st.avgSessionMinutes.toInt()) else "\u2013")
            StatRow("Goal completion rate", "${(st.completionRate * 100).toInt()}%")
            StatRow("Days goal achieved", st.goalDays.toString())
            StatRow("Current streak", "${s.streaks.current} days")
            StatRow("Longest streak", "${s.streaks.longest} days")
            StatRow("Books completed (all time)", s.booksCompleted.toString())
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (month) "Records \u2013 " + s.selectedDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                 else "Records \u2013 " + s.ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (month) TextButton(onClick = { adding = true }) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Add") }
        }
        if (s.records.isEmpty()) Text("Nothing logged " + (if (month) "on this day." else "this month."), Modifier.padding(16.dp), color = TextSecondary)
        s.records.forEach { r ->
            RecordRow(r, s.books.firstOrNull { it.book.id == r.bookId }?.book?.title, cfg.time24h, cfg.compactRecords,
                onEdit = { editing = r }, onDelete = { deleting = r })
        }
        Spacer(Modifier.height(24.dp))
    }

    if (adding) {
        // Same time of day as now, on the selected date (or noon if that date has no "now").
        val ts = ZonedDateTime.of(s.selectedDay, LocalTime.now().withSecond(0).withNano(0), ZoneId.systemDefault()).toInstant().toEpochMilli()
        LogDialog(books = s.books, initialTimestamp = minOf(ts, System.currentTimeMillis()), defaultBookId = cfg.defaultBookId, is24h = cfg.time24h,
            typicalMinutes = cfg.defaultSessionMinutes, onDismiss = { adding = false }, onSave = { vm.add(it); adding = false })
    }
    editing?.let { r -> LogDialog(books = s.books, initial = r, is24h = cfg.time24h, typicalMinutes = cfg.defaultSessionMinutes,
        onDismiss = { editing = null }, onSave = { vm.edit(r, it); editing = null }) }
    deleting?.let { r ->
        ConfirmDialog("Delete this record?", "${r.pagesRead} pages will be removed; charts, totals and streaks are recalculated.", "Delete",
            onConfirm = { vm.delete(r); deleting = null }, onDismiss = { deleting = null })
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
