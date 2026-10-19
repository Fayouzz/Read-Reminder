package com.personal.readreminder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.ReadingRepository
import com.personal.readreminder.data.SettingsRepository
import com.personal.readreminder.domain.DayBoundary
import com.personal.readreminder.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class SettingsViewModel(private val repo: ReadingRepository, private val settings: SettingsRepository) : ViewModel() {
    val dailyGoal = settings.settings.map { it.dailyGoal }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 10)
    fun setGoal(pages: Int) = viewModelScope.launch {
        val reset = settings.settings.first().resetMinutes
        repo.setDailyGoal(pages, DayBoundary.logicalDate(Instant.now(), ZoneId.systemDefault(), reset))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val goal by vm.dailyGoal.collectAsStateWithLifecycle()
    var custom by remember { mutableStateOf("") }
    val presets = listOf(5, 10, 15, 20, 25, 30, 50)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("READING SETTINGS", color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Text("Daily page goal: $goal pages", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { FilterChip(selected = goal == it, onClick = { vm.setGoal(it) }, label = { Text("$it/day") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(4) }, label = { Text("Custom goal") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(enabled = (custom.toIntOrNull() ?: 0) > 0, onClick = { vm.setGoal(custom.toInt()); custom = "" }) { Text("Set") }
        }
        Text("Changing the goal affects today and future days only; past days keep the goal they had.", color = TextSecondary)
        HorizontalDivider()
        Text("Reminders, themes, backup/restore and the remaining settings arrive in Stages 5\u20136.", color = TextSecondary)
    }
}
