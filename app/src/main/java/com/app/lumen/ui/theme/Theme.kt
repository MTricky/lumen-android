package com.app.lumen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NearBlack = Color(0xFF1A1A2E)
val SoftGold = Color(0xFFD4AF37)
val MarianBlue = Color(0xFF1E3A5F)
val SoftIvory = Color(0xFFFAF9F6)
val Slate = Color(0xFF64748B)
val CardBackground = Color(0xFF0E0E1E)

val LiturgicalGreen = Color(0xFF2E7D32)
val LiturgicalPurple = Color(0xFF6A1B9A)
val LiturgicalWhite = Color(0xFFF5F5F5)
val LiturgicalRed = Color(0xFFC62828)
val LiturgicalRose = Color(0xFFE91E63)

private val DarkColorScheme = darkColorScheme(
    primary = SoftGold,
    secondary = MarianBlue,
    background = NearBlack,
    surface = CardBackground,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun LumenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
