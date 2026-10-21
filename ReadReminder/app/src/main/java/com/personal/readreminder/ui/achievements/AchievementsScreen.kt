package com.personal.readreminder.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.ReadingRepository
import com.personal.readreminder.domain.AchievementType
import com.personal.readreminder.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class AchievementsViewModel(repo: ReadingRepository) : ViewModel() {
    val unlocked: StateFlow<Map<String, Long>> = repo.observeAchievements().map { l -> l.associate { it.achievementType to it.unlockedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

@Composable
fun AchievementsScreen(vm: AchievementsViewModel, onBack: () -> Unit) {
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val all = AchievementType.entries
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Column {
                Text("Achievements", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${all.count { it.name in unlocked }} of ${all.size} unlocked", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
        }
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(all) { a ->
                val at = unlocked[a.name]
                val done = at != null
                val desc = a.title + ": " + (if (done) "unlocked" else "locked. " + a.description)
                Card(Modifier.fillMaxWidth().semantics { contentDescription = desc },
                    colors = CardDefaults.cardColors(containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(if (done) PrimaryBlue.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(if (done) a.emoji else "\uD83D\uDD12", fontSize = 28.sp, modifier = Modifier.alpha(if (done) 1f else 0.6f))
                        }
                        Text(a.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp)
                        Text(if (done) "Unlocked " + Instant.ofEpochMilli(at!!).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) else a.description,
                            fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
