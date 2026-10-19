package com.personal.readreminder.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.ui.theme.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var showLog by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ReadingSessionEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val timeFmt = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Good morning!"; hour < 18 -> "Good afternoon!"; else -> "Good evening!" }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PrimaryBlue, BackgroundBlue))).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Mascot(Modifier.size(72.dp))
                        Spacer(Modifier.width(12.dp))
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
                            Column(Modifier.padding(12.dp)) {
                                Text(greeting, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(s.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), color = TextSecondary, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(s.message, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(s.progress, s.pagesToday, s.goal)
                    Spacer(Modifier.height(8.dp))
                    Text(if (s.goalReached) "Daily goal achieved \u2713" else "${s.percent}% completed",
                        color = if (s.goalReached) Success else TextSecondary, fontWeight = FontWeight.SemiBold)
                    Text("Current streak: ${s.currentStreak} day${if (s.currentStreak == 1) "" else "s"}  \u00B7  Longest: ${s.longestStreak}",
                        color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showLog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(26.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Log Reading", fontSize = 16.sp)
                    }
                }
            }
            item { Text("Today's records", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (s.sessions.isEmpty()) {
                item { Text("No reading logged yet today. Tap \u201CLog Reading\u201D after your next few pages.", Modifier.padding(16.dp), color = TextSecondary) }
            }
            items(s.sessions, key = { it.id }) { r ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Instant.ofEpochMilli(r.startTimestamp).atZone(ZoneId.systemDefault()).format(timeFmt), Modifier.width(84.dp), color = TextSecondary)
                    Text("${r.pagesRead} pages", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { toDelete = r }) { Icon(Icons.Default.Delete, contentDescription = "Delete reading record", tint = TextSecondary) }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Divider)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showLog) LogDialog(onDismiss = { showLog = false }, onSave = { vm.log(it); showLog = false })
    toDelete?.let { r ->
        AlertDialog(onDismissRequest = { toDelete = null },
            title = { Text("Delete this record?") }, text = { Text("${r.pagesRead} pages will be removed and your totals and streak recalculated.") },
            confirmButton = { TextButton(onClick = { vm.delete(r); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } })
    }
}

@Composable
private fun LogDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val pages = text.toIntOrNull()
    val error = text.isNotEmpty() && (pages == null || pages <= 0)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Log reading") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pages you read in this session:", color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 20, 25).forEach { n ->
                        AssistChip(onClick = { text = ((pages ?: 0) + n).toString() }, label = { Text("+$n") })
                    }
                }
                OutlinedTextField(text, { text = it.filter(Char::isDigit).take(4) }, label = { Text("Pages") }, singleLine = true,
                    isError = error, supportingText = { if (error) Text("Enter a whole number greater than 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(enabled = pages != null && pages > 0, onClick = { onSave(pages!!) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun ProgressRing(progress: Float, pages: Int, goal: Int) {
    val anim by animateFloatAsState(progress, tween(600), label = "ring")
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 22.dp.toPx(); val inset = w / 2
            val sz = Size(size.width - w, size.height - w)
            drawArc(Divider, -90f, 360f, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
            if (anim > 0f) drawArc(PrimaryBlue, -90f, 360f * anim, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pages / $goal", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("pages today", color = TextSecondary)
        }
    }
}

/** Original mascot: a friendly blue blob holding a little book. */
@Composable
fun Mascot(modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.Transparent, CircleShape)) {
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
