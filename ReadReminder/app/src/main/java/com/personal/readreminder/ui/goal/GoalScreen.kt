package com.personal.readreminder.ui.goal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.readreminder.ui.common.NumberDialog
import com.personal.readreminder.ui.common.WeekStrip
import com.personal.readreminder.ui.home.HomeViewModel
import com.personal.readreminder.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalScreen(vm: HomeViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var custom by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Daily goal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Daily reading goal", color = TextSecondary)
            Text("${s.goal} pages", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            BookStack(s.progress)
            Text(if (s.goalReached) "Goal achieved \u2713 \u2014 ${s.pagesToday} pages today" else "${s.pagesToday} of ${s.goal} pages today \u00B7 ${s.remaining} to go",
                fontWeight = FontWeight.SemiBold, color = if (s.goalReached) Success else MaterialTheme.colorScheme.onSurface)
            Text("${s.percent}% completed", color = TextSecondary)
            HorizontalDivider()
            Text("This week: ${s.weekGoalDays} of 7 days goal achieved", fontWeight = FontWeight.SemiBold)
            WeekStrip(s.week)
            HorizontalDivider()
            Text("Change your goal", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 25, 30, 50).forEach { FilterChip(selected = s.goal == it, onClick = { vm.setGoal(it) }, label = { Text("$it/day") }) }
                FilterChip(selected = s.goal !in listOf(5, 10, 15, 20, 25, 30, 50), onClick = { custom = true }, label = { Text("Custom") })
            }
            Text("Changing the goal affects today and future days only; past days keep the goal they had.", color = TextSecondary, fontSize = 13.sp)
        }
    }
    if (custom) NumberDialog("Custom daily goal", "Pages per day", s.goal, 1, 1000, onSave = { vm.setGoal(it); custom = false }, onDismiss = { custom = false })
}

/** Ten page-blocks stacked up as the goal fills. */
@Composable
private fun BookStack(progress: Float) {
    val filled = (progress * 10).toInt().coerceIn(0, 10)
    val off = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val bw = size.width * 0.5f; val bh = size.height / 11f
        val left = (size.width - bw) / 2
        for (i in 0 until 10) {
            val y = size.height - (i + 1) * bh
            drawRoundRect(if (i < filled) PrimaryBlue else off, Offset(left + (if (i % 2 == 0) 0f else 8.dp.toPx()), y + 1.5f), Size(bw, bh - 3f), CornerRadius(4.dp.toPx()))
        }
    }
}
