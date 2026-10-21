package com.personal.readreminder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.*
import com.personal.readreminder.ui.common.*
import com.personal.readreminder.ui.theme.*

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), Modifier.padding(start = 16.dp, top = 22.dp, bottom = 4.dp), color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ValueRow(title: String, value: String = "", subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (value.isNotEmpty()) Text(value, color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, onOpenReminders: () -> Unit, onOpenGoal: () -> Unit) {
    val ctx = LocalContext.current
    val cfg by vm.cfg.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val csvOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u -> if (u != null) vm.exportCsv(u) }
    val backupOut = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> if (u != null) vm.exportBackup(u) }
    val restoreIn = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) vm.restore(u) }

    val is24 = cfg.time24h
    fun close() { dialog = null }
    fun time(title: String, minutes: Int, onPick: (Int) -> Unit): () -> Unit = { pickTime(ctx, minutes, is24, onPick) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    val bookName = books.firstOrNull { it.book.id == cfg.defaultBookId }?.book?.title ?: "Automatic"

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().background(PrimaryBlue).padding(16.dp)) {
                Text("Advanced Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Section("Reminder settings")
            ValueRow("Reminder schedule", onClick = onOpenReminders)
            SwitchRow("All reminders", "Master switch", cfg.remindersEnabled) { vm.set(K.remEnabled, it) }
            ValueRow("Default reminder message", cfg.defaultMessage.ifBlank { "Varied" }) {
                dialog = { TextDialog("Default reminder message", "Message (empty = varied)", cfg.defaultMessage, 120, { vm.set(K.defMsg, it); close() }, ::close) }
            }
            ValueRow("Reminder sound / tone", cfg.tone.label) {
                dialog = { ChoiceDialog("Reminder sound", NotifTone.entries.map { it to it.label }, cfg.tone, { vm.set(K.tone, it.name); close() }, ::close) }
            }
            SwitchRow("Vibration", null, cfg.vibrate) { vm.set(K.vibrate, it) }
            SwitchRow("Reminder alert", "Play sound and vibrate. Off = notifications arrive silently.", cfg.alertEnabled) { vm.set(K.alert, it) }
            ValueRow("Reminder mode", cfg.mode.label, when (cfg.mode) {
                ReminderMode.FIXED -> "Notify at the times in your schedule"
                ReminderMode.INTERVAL -> "Notify every interval during active hours"
                ReminderMode.ADAPTIVE -> "Schedule times, skipping ones soon after you read, plus a last call before active hours end"
            }) {
                dialog = { ChoiceDialog("Reminder mode", ReminderMode.entries.map { it to it.label }, cfg.mode, { vm.set(K.mode, it.name); close() }, ::close) }
            }
            ValueRow("Reminder interval", "${cfg.intervalMinutes} min", "Used in Interval mode") {
                dialog = { ChoiceDialog("Reminder interval", listOf(30, 45, 60, 90, 120, 180, 240).map { it to "Every $it min" }, cfg.intervalMinutes, { vm.set(K.interval, it); close() }, ::close) }
            }
            ValueRow("Active hours: start", fmtMinutes(cfg.activeStart, is24), "Interval mode only", time("", cfg.activeStart) { vm.set(K.actStart, it) })
            ValueRow("Active hours: end", fmtMinutes(cfg.activeEnd, is24), null, time("", cfg.activeEnd) { vm.set(K.actEnd, it) })
            SwitchRow("Quiet hours", "No normal reminders in this period (can cross midnight)", cfg.quietEnabled) { vm.set(K.quietOn, it) }
            ValueRow("Quiet hours: start", fmtMinutes(cfg.quietStart, is24), null, time("", cfg.quietStart) { vm.set(K.quietStart, it) })
            ValueRow("Quiet hours: end", fmtMinutes(cfg.quietEnd, is24), null, time("", cfg.quietEnd) { vm.set(K.quietEnd, it) })
            SwitchRow("Follow-up reminder", "One extra nudge 30 minutes after a reminder you ignored", cfg.remindMissed) { vm.set(K.missed, it) }
            SwitchRow("Stop reminders after daily goal", "No more reminders today once you reach your goal", cfg.stopAfterGoal) { vm.set(K.stopGoal, it) }
            SwitchRow("Fewer reminders after reading", "Skip a reminder within an hour of logging a session", cfg.reduceAfterLog) { vm.set(K.reduce, it) }
            ValueRow("Snooze duration", "${cfg.snoozeMinutes} min") {
                dialog = { ChoiceDialog("Snooze duration", listOf(5, 10, 15, 20, 30, 60).map { it to "$it minutes" }, cfg.snoozeMinutes, { vm.set(K.snooze, it); close() }, ::close) }
            }
            ValueRow("Pages per reminder session", "${cfg.defaultTargetPages} pages", "A target only \u2014 never logged automatically") {
                dialog = { NumberDialog("Pages per session", "Pages", cfg.defaultTargetPages, 1, 500, { vm.set(K.targetPages, it); close() }, ::close) }
            }

            Section("Reading settings")
            ValueRow("Daily page goal", "${cfg.dailyGoal} pages", onClick = onOpenGoal)
            ValueRow("Preferred reading start", fmtMinutes(cfg.preferredStart, is24), null, time("", cfg.preferredStart) { vm.set(K.prefStart, it) })
            ValueRow("Preferred reading end", fmtMinutes(cfg.preferredEnd, is24), null, time("", cfg.preferredEnd) { vm.set(K.prefEnd, it) })
            ValueRow("Default book", bookName) {
                val opts = listOf(-1L to "Automatic (latest book)") + books.filter { it.book.status == BookStatus.READING }.map { it.book.id to it.book.title }
                dialog = { ChoiceDialog("Default book", opts, cfg.defaultBookId, { vm.set(K.defBook, it); close() }, ::close) }
            }
            ValueRow("Default session duration", "${cfg.defaultSessionMinutes} min", "Shown as a hint when logging") {
                dialog = { NumberDialog("Default session duration", "Minutes", cfg.defaultSessionMinutes, 1, 600, { vm.set(K.defMinutes, it); close() }, ::close) }
            }
            ValueRow("Week starts on", if (cfg.weekStartMonday) "Monday" else "Sunday") {
                dialog = { ChoiceDialog("Week starts on", listOf(true to "Monday", false to "Sunday"), cfg.weekStartMonday, { vm.set(K.weekMon, it); close() }, ::close) }
            }
            ValueRow("Daily goal reset time", fmtMinutes(cfg.resetMinutes, is24), "Start of your reading day (default midnight). Existing records are re-assigned.",
                time("", cfg.resetMinutes) { vm.setReset(it) })

            Section("Statistics and display")
            ValueRow("Dashboard metric", if (cfg.dashboardMinutes) "Minutes" else "Pages", "Goals always use pages") {
                dialog = { ChoiceDialog("Dashboard metric", listOf(false to "Pages", true to "Minutes (pages still drive the ring)"), cfg.dashboardMinutes, { vm.set(K.metricMin, it); close() }, ::close) }
            }
            ValueRow("Time format", if (cfg.time24h) "24-hour" else "12-hour") {
                dialog = { ChoiceDialog("Time format", listOf(false to "12-hour", true to "24-hour"), cfg.time24h, { vm.set(K.time24, it); close() }, ::close) }
            }
            ValueRow("Theme", cfg.theme.label) {
                dialog = { ChoiceDialog("Theme", ThemeMode.entries.map { it to it.label }, cfg.theme, { vm.set(K.theme, it.name); close() }, ::close) }
            }
            ValueRow("History opens in", if (cfg.historyDefaultYear) "Year" else "Month") {
                dialog = { ChoiceDialog("History opens in", listOf(false to "Month", true to "Year"), cfg.historyDefaultYear, { vm.set(K.histYear, it); close() }, ::close) }
            }
            SwitchRow("Compact reading records", "One line per record", cfg.compactRecords) { vm.set(K.compact, it) }

            Section("Personalization")
            ValueRow("Display name", cfg.displayName.ifBlank { "Not set" }) {
                dialog = { TextDialog("Display name", "Name (optional)", cfg.displayName, 30, { vm.set(K.name, it); close() }, ::close) }
            }
            ValueRow("Motivation style", cfg.motivation.label, "Tone of reminder messages") {
                dialog = { ChoiceDialog("Motivation style", MotivationStyle.entries.map { it to it.label }, cfg.motivation, { vm.set(K.motivation, it.name); close() }, ::close) }
            }
            ValueRow("Daily reading target message", cfg.targetMessage.ifBlank { "Not set" }, "Shown on the Home screen") {
                dialog = { TextDialog("Target message", "e.g. Finish my book by May", cfg.targetMessage, 80, { vm.set(K.targetMsg, it); close() }, ::close) }
            }

            Section("Data management")
            ValueRow("Export reading history (CSV)", onClick = { csvOut.launch("read-reminder-history.csv") })
            ValueRow("Back up everything (JSON)", onClick = { backupOut.launch("read-reminder-backup.json") })
            ValueRow("Restore from backup", subtitle = "Replaces all current data with the backup") {
                dialog = { ConfirmDialog("Restore from backup?", "All current books, records, reminders and settings are replaced by the backup. The file is checked first; if it's invalid nothing changes.",
                    "Choose file", { close(); restoreIn.launch(arrayOf("*/*")) }, ::close) }
            }
            ValueRow("Reset statistics", subtitle = "Deletes all reading records and achievements; keeps books, reminders and settings") {
                dialog = { ConfirmDialog("Reset statistics?", "All reading records, daily goal history and achievements will be permanently deleted.", "Reset", { vm.resetStats(); close() }, ::close) }
            }
            ValueRow("Delete all data", subtitle = "Removes everything stored by this app") {
                dialog = { ConfirmDialog("Delete all data?", "Books, records, reminders and settings will be permanently deleted. This can't be undone.", "Delete everything", { vm.deleteAll(); close() }, ::close) }
            }
            ValueRow("About Read Reminder") {
                dialog = { AlertDialog(onDismissRequest = ::close, title = { Text("Read Reminder") },
                    text = { Text("A personal reading habit tracker. Everything stays on this phone: no account, no internet, no analytics.\n\nVersion $version") },
                    confirmButton = { TextButton(onClick = ::close) { Text("OK") } }) }
            }
            ValueRow("App version", version)
            Spacer(Modifier.height(32.dp))
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
    dialog?.invoke()
}
