package com.personal.readreminder.ui.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.ReminderEntity
import com.personal.readreminder.reminders.NotificationHelper
import com.personal.readreminder.ui.common.*
import com.personal.readreminder.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun weekdaysFor(mask: Int): String {
    if (mask == 127) return "Every day"
    return DayOfWeek.entries.filter { (mask and (1 shl (it.value - 1))) != 0 }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

@Composable
fun ReminderScreen(vm: ReminderViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val list by vm.reminders.collectAsStateWithLifecycle()
    val cfg by vm.cfg.collectAsStateWithLifecycle()
    val rt by vm.runtime.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<ReminderEntity?>(null) }
    var notifOk by remember { mutableStateOf(NotificationHelper.canPost(ctx)) }
    var exactOk by remember { mutableStateOf(vm.canExact()) }
    val snack = remember { SnackbarHostState() }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = NotificationHelper.canPost(ctx) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notifOk = NotificationHelper.canPost(ctx); exactOk = vm.canExact() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(PrimaryBlue).padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                        Column {
                            Text("Reminder Schedule", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Build a daily reading habit", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reminders are based on the times below and your quiet hours, goal and recent reading (see Settings). We never log pages just because a reminder appeared.", color = TextSecondary, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("All reminders", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = cfg.remindersEnabled, onCheckedChange = { vm.setMaster(it) })
                    }
                    if (cfg.remindersEnabled) {
                        Text(if (rt.nextAt > 0) "Next reminder: " + nextText(rt.nextAt, cfg.time24h) else "No upcoming reminder (check times, quiet hours and mode).", fontSize = 13.sp)
                    }
                    if (!notifOk) PermissionCard("Notifications are off",
                        "Android is blocking notifications for this app, so reminders can't appear. Allow them to get reminded.",
                        if (Build.VERSION.SDK_INT >= 33) "Allow notifications" else "Open settings") {
                        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                    }
                    if (!exactOk) PermissionCard("Exact timing is off",
                        "Without \u201CAlarms & reminders\u201D access, Android may delay reminders by several minutes or more. Allowing it makes them arrive on time.",
                        "Allow exact reminders") {
                        if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pickTime(ctx, 19 * 60 + 30, cfg.time24h) { vm.add(it) } }, shape = RoundedCornerShape(24.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Add reminder")
                        }
                        OutlinedButton(onClick = { vm.sendTest() }, shape = RoundedCornerShape(24.dp)) { Text("Send test") }
                    }
                }
            }
            if (list.isEmpty()) item { Text("No reminders yet. Tap \u201CAdd reminder\u201D to pick a time.", Modifier.padding(16.dp), color = TextSecondary) }
            items(list, key = { it.id }) { r ->
                ReminderRow(r, cfg.time24h, cfg.weekStartMonday, cfg.defaultMessage, expanded == r.id,
                    onToggleExpand = { expanded = if (expanded == r.id) null else r.id },
                    onChange = { vm.update(it) }, onDelete = { deleting = r })
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
    deleting?.let { r ->
        ConfirmDialog("Delete reminder?", "The ${fmtMinutes(r.hour * 60 + r.minute, cfg.time24h)} reminder will be removed.", "Delete",
            onConfirm = { vm.delete(r); deleting = null }, onDismiss = { deleting = null })
    }
}

private fun nextText(at: Long, is24h: Boolean): String {
    val z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val day = when (z.toLocalDate()) { today -> "today"; today.plusDays(1) -> "tomorrow"; else -> z.format(DateTimeFormatter.ofPattern("EEE d MMM")) }
    return fmtInstant(at, is24h) + " " + day
}

@Composable
private fun PermissionCard(title: String, body: String, button: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.18f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 13.sp)
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun ReminderRow(
    r: ReminderEntity, is24h: Boolean, mondayStart: Boolean, defaultMessage: String, expanded: Boolean,
    onToggleExpand: () -> Unit, onChange: (ReminderEntity) -> Unit, onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val minutes = r.hour * 60 + r.minute
    Column(Modifier.fillMaxWidth().background(if (expanded) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fmtMinutes(minutes, is24h), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(weekdaysFor(r.weekdayMask), color = TextSecondary, fontSize = 13.sp)
            }
            Switch(checked = r.enabled, onCheckedChange = { onChange(r.copy(enabled = it)) })
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand")
        }
        if (expanded) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val order = if (mondayStart) DayOfWeek.entries else listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    order.forEach { d ->
                        val bit = 1 shl (d.value - 1)
                        val on = (r.weekdayMask and bit) != 0
                        val name = d.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(if (on) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClickLabel = (if (on) "Remove " else "Add ") + name) {
                                    val m = r.weekdayMask xor bit
                                    if (m != 0) onChange(r.copy(weekdayMask = m))   // keep at least one day
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = if (on) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickTime(ctx, minutes, is24h) { m -> onChange(r.copy(hour = m / 60, minute = m % 60)) } }) { Text("Change time") }
                    OutlinedButton(onClick = { onChange(r.copy(weekdayMask = 127)) }) { Text("Every day") }
                }
                var msg by remember(r.id, r.message) { mutableStateOf(r.message ?: "") }
                OutlinedTextField(msg, { msg = it.take(120) }, label = { Text("Custom message (optional)") },
                    placeholder = { Text(if (defaultMessage.isNotBlank()) defaultMessage else "A friendly message is chosen for you") },
                    modifier = Modifier.fillMaxWidth())
                var pages by remember(r.id, r.targetPages) { mutableStateOf(r.targetPages?.toString() ?: "") }
                OutlinedTextField(pages, { pages = it.filter(Char::isDigit).take(3) }, label = { Text("Target pages for this session (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onChange(r.copy(message = msg.trim().ifBlank { null }, targetPages = pages.toIntOrNull()?.takeIf { it > 0 })) }) { Text("Save details") }
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
