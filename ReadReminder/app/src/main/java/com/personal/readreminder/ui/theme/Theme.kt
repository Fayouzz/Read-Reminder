package com.personal.readreminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.personal.readreminder.data.ThemeMode

val PrimaryBlue = Color(0xFF55AEF5)
val LightBlue = Color(0xFF8CCBFA)
val BackgroundBlue = Color(0xFF65B8F0)
val TextMain = Color(0xFF20252B)
val TextSecondary = Color(0xFF80868B)
val Success = Color(0xFF35B86B)
val Warning = Color(0xFFFFB547)
val Divider = Color(0xFFE6E9ED)

private val LightScheme = lightColorScheme(
    primary = PrimaryBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F3FD), onPrimaryContainer = TextMain,
    background = Color.White, onBackground = TextMain,
    surface = Color.White, onSurface = TextMain, onSurfaceVariant = TextSecondary,
    outlineVariant = Divider, tertiary = Success,
)

private val DarkScheme = darkColorScheme(
    primary = PrimaryBlue, onPrimary = Color(0xFF06223A),
    primaryContainer = Color(0xFF1F3A52), onPrimaryContainer = Color(0xFFE8EAED),
    background = Color(0xFF11161B), onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1A2027), onSurface = Color(0xFFE8EAED), onSurfaceVariant = Color(0xFF9AA0A6),
    surfaceVariant = Color(0xFF232B33),
    outlineVariant = Color(0xFF2B333B), tertiary = Success,
)

@Composable
fun ReadReminderTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
