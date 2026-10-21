package com.personal.readreminder.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.personal.readreminder.ui.theme.LightBlue
import com.personal.readreminder.ui.theme.PrimaryBlue
import com.personal.readreminder.ui.theme.Success

/** Simple tappable bar chart drawn on a Canvas. [goal] draws a dashed reference line. */
@Composable
fun BarChart(
    values: List<Int>, selected: Int, goal: Int?, xLabel: (Int) -> String, labelEvery: Int,
    onSelect: (Int) -> Unit, description: String, modifier: Modifier = Modifier,
) {
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val paint = remember { android.graphics.Paint().apply { isAntiAlias = true } }
    val maxV = maxOf(values.maxOrNull() ?: 0, goal ?: 0, 1)
    val top = niceMax(maxV)
    Canvas(
        modifier.fillMaxWidth().height(230.dp).semantics { contentDescription = description }
            .pointerInput(values.size) {
                detectTapGestures { off ->
                    val left = 36.dp.toPx()
                    if (off.x >= left && values.isNotEmpty()) {
                        val i = ((off.x - left) / ((size.width - left) / values.size)).toInt()
                        if (i in values.indices) onSelect(i)
                    }
                }
            },
    ) {
        val left = 36.dp.toPx(); val bottom = size.height - 22.dp.toPx(); val topPad = 8.dp.toPx()
        val h = bottom - topPad; val slot = (size.width - left) / maxOf(values.size, 1)
        paint.color = axis.toArgb(); paint.textSize = 10.dp.toPx()
        // grid + y labels
        listOf(0, top / 2, top).forEach { v ->
            val y = bottom - h * v / top
            drawLine(grid, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            drawIntoCanvas { it.nativeCanvas.drawText("$v", left - 6.dp.toPx(), y + 4.dp.toPx(), paint) }
        }
        // bars
        values.forEachIndexed { i, v ->
            val bh = h * v / top
            val color = when { i == selected -> PrimaryBlue; goal != null && v >= goal && v > 0 -> Success; else -> LightBlue }
            if (v > 0) drawRoundRect(color, Offset(left + i * slot + slot * 0.2f, bottom - bh), Size(slot * 0.6f, bh), CornerRadius(3.dp.toPx()))
            else if (i == selected) drawRoundRect(PrimaryBlue, Offset(left + i * slot + slot * 0.2f, bottom - 3.dp.toPx()), Size(slot * 0.6f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            if (labelEvery > 0 && (i % labelEvery == 0 || i == selected)) {
                paint.textAlign = android.graphics.Paint.Align.CENTER
                drawIntoCanvas { it.nativeCanvas.drawText(xLabel(i), left + i * slot + slot / 2, size.height - 6.dp.toPx(), paint) }
            }
        }
        // goal reference line
        if (goal != null && goal > 0) {
            val y = bottom - h * goal / top
            drawLine(Success, Offset(left, y), Offset(size.width, y), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        }
    }
}

private fun niceMax(v: Int): Int {
    if (v <= 4) return 4
    val mag = Math.pow(10.0, Math.floor(Math.log10(v.toDouble()))).toInt()
    for (m in listOf(1, 2, 4, 5, 10)) { if (m * mag >= v) return maxOf(2, m * mag) }
    return v
}
