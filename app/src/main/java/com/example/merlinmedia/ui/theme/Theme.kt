package com.example.merlinmedia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cinematic Solid Slate Palette (No glassmorphism, no blur, high contrast & readability)
val BgDark = Color(0xFF0C0E14)
val NavRailBg = Color(0xFF121620)
val SurfaceDark = Color(0xFF151924)
val CardSurface = Color(0xFF181D28)
val CardSurfaceFocused = Color(0xFF222938)
val BorderSubtle = Color(0xFF262E3E)
val FocusRingColor = Color(0xFF38BDF8)
val PrimaryBlue = Color(0xFF2563EB)
val AccentSky = Color(0xFF38BDF8)
val AccentGold = Color(0xFFF59E0B)
val LiveBadgeColor = Color(0xFFE63946)
val ErrorRed = Color(0xFFFF334B)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val SolidBarBg = Color(0xFF10141E)

private val DarkColorScheme = darkColorScheme(
    primary = AccentSky,
    onPrimary = Color.Black,
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed
)

@Composable
fun MerlinTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}