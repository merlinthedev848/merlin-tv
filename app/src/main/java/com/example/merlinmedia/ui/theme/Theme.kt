package com.example.merlinmedia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cobra / TiviMate Dark Palette & High-Contrast Accents
val BgDark = Color(0xFF0A0D14)
val NavRailBg = Color(0xFF0F131C)
val SurfaceDark = Color(0xFF141A26)
val CardSurface = Color(0xFF182030)
val CardSurfaceFocused = Color(0xFF1E293B)
val BorderSubtle = Color(0xFF263348)
val FocusRingColor = Color(0xFF38BDF8)
val PrimaryBlue = Color(0xFF0284C7)
val AccentSky = Color(0xFF38BDF8)
val PrimaryCyan = Color(0xFF38BDF8)
val SecondaryTeal = Color(0xFF0EA5E9)
val AccentGold = Color(0xFFF59E0B)
val LiveBadgeColor = Color(0xFFE63946)
val ErrorRed = Color(0xFFFF334B)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val SolidBarBg = Color(0xFF0F1420)
val FocusedBorderColor = FocusRingColor

// Glassmorphism & Quality Badges
val GlassSurfaceDark = Color(0xEB0D121D)
val GlassBorder = Color(0x3338BDF8)
val QualityFhdBadge = Color(0xFF0284C7)
val Quality4kBadge = Color(0xFF7C3AED)
val EpgProgressTrack = Color(0xFF1E293B)
val EpgProgressFill = Color(0xFF38BDF8)

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