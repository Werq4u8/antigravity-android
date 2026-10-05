package com.google.antigravity.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0F1013)
val SidebarDark = Color(0xFF141519)
val CardDark = Color(0xFF1A1C21)
val CardDarkVariant = Color(0xFF23252C)
val BorderDark = Color(0xFF2A2D36)

val AntigravityBlue = Color(0xFF3B82F6)
val GoogleBlue = Color(0xFF4285F4)
val GoogleGreen = Color(0xFF34D399)
val GoogleYellow = Color(0xFFFBBF24)
val GoogleRed = Color(0xFFF87171)

val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    primary = AntigravityBlue,
    secondary = GoogleGreen,
    tertiary = GoogleYellow,
    background = BgDark,
    surface = CardDark,
    surfaceVariant = CardDarkVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderDark,
    error = GoogleRed
)

@Composable
fun AntigravityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
