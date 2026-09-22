package com.example.merlinmedia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// ─── Color Palette ───
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

// ─── Glassmorphism & Quality Badges ───
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
    tertiary = AccentGold,
    onTertiary = Color.Black,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = BorderSubtle,
    outlineVariant = GlassBorder
)

private val MerlinTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = TextMuted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        color = TextMuted
    )
)

private val MerlinShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MerlinTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MerlinTypography,
        shapes = MerlinShapes,
        content = content
    )
}