package com.personal.readreminder.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF55AEF5)
val LightBlue = Color(0xFF8CCBFA)
val BackgroundBlue = Color(0xFF65B8F0)
val TextMain = Color(0xFF20252B)
val TextSecondary = Color(0xFF80868B)
val Success = Color(0xFF35B86B)
val Warning = Color(0xFFFFB547)
val Divider = Color(0xFFE6E9ED)

private val Scheme = lightColorScheme(
    primary = PrimaryBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F3FD), onPrimaryContainer = TextMain,
    background = Color.White, onBackground = TextMain,
    surface = Color.White, onSurface = TextMain, onSurfaceVariant = TextSecondary,
    outlineVariant = Divider, tertiary = Success,
)

@Composable
fun ReadReminderTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
