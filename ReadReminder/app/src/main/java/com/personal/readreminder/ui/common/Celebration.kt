package com.personal.readreminder.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.personal.readreminder.ui.theme.*

/** One short celebration: a bouncing emoji plus falling confetti that fades out (about 1.4 s). */
@Composable
fun Celebration(emoji: String, title: String, subtitle: String, onDismiss: () -> Unit) {
    val fall = remember { Animatable(0f) }
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    LaunchedEffect(Unit) { fall.animateTo(1f, tween(1400)) }
    val colors = listOf(PrimaryBlue, Success, Warning, LightBlue, Color(0xFFE57373))
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Box {
                Canvas(Modifier.matchParentSize()) {
                    for (i in 0 until 28) {
                        val x = size.width * (((i * 37) % 100) / 100f)
                        val y = size.height * fall.value * (0.45f + (i % 5) / 8f) - 10f
                        drawCircle(colors[i % colors.size], radius = 4.dp.toPx() + (i % 3) * 1.5.dp.toPx(), center = Offset(x, y), alpha = (1f - fall.value * 0.8f))
                    }
                }
                Column(Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(emoji, fontSize = (56 * scale.value).sp)
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(subtitle, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = onDismiss) { Text("Nice!") }
                }
            }
        }
    }
}
