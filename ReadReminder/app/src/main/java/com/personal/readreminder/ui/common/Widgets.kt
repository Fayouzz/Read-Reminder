package com.personal.readreminder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.readreminder.data.ReadingSessionEntity
import com.personal.readreminder.domain.DayState
import com.personal.readreminder.domain.DayStatus
import com.personal.readreminder.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RecordRow(r: ReadingSessionEntity, bookTitle: String?, is24h: Boolean, compact: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 0.dp else 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmtInstant(r.startTimestamp, is24h), Modifier.width(84.dp), color = TextSecondary, fontSize = 13.sp)
        Column(Modifier.weight(1f)) {
            val range = if (r.startPage != null && r.endPage != null) "p. ${r.startPage}\u2013${r.endPage}" else null
            val detail = listOfNotNull(bookTitle, r.durationMinutes?.let { "$it min" }, range).joinToString(" \u00B7 ")
            if (compact) {
                Text("${r.pagesRead} pages" + (if (detail.isNotEmpty()) " \u00B7 $detail" else ""), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            } else {
                Text("${r.pagesRead} pages", fontWeight = FontWeight.SemiBold)
                if (detail.isNotEmpty()) Text(detail, color = TextSecondary, fontSize = 12.sp)
                if (!r.notes.isNullOrBlank()) Text(r.notes, color = TextSecondary, fontSize = 12.sp, maxLines = 2)
            }
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit reading record", tint = TextSecondary) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete reading record", tint = TextSecondary) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** Seven circles: achieved (green check), some progress (blue, pages), nothing logged (grey). */
@Composable
fun WeekStrip(days: List<DayStatus>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { d ->
            val (bg, fg) = when (d.state) {
                DayState.ACHIEVED -> Success to Color.White
                DayState.PARTIAL -> LightBlue to TextMain
                DayState.NONE -> Color(0xFFB8BEC4) to Color.White
            }
            val desc = d.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ": " +
                when (d.state) { DayState.ACHIEVED -> "goal achieved"; DayState.PARTIAL -> "${d.pages} pages"; DayState.NONE -> "no reading" }
            Column(Modifier.semantics { contentDescription = desc }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                    when (d.state) {
                        DayState.ACHIEVED -> Icon(Icons.Default.Check, contentDescription = null, tint = fg)
                        DayState.PARTIAL -> Text("${d.pages}", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        DayState.NONE -> Text("\u2013", color = fg)
                    }
                }
                Text(d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
