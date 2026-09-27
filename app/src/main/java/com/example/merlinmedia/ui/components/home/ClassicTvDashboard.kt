package com.example.merlinmedia.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-end Classic Honeycomb Dashboard with Rounded Square (Squircle) Tiles.
 */
@Composable
fun ClassicTvDashboard(
    currentTime: String,
    availableUpdate: UpdateInfo?,
    isOnline: Boolean,
    onOpenSection: (NavSection) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSports: () -> Unit,
    onOpenCatchUp: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
    ) {
        // 1. Honeycomb Geometric Hexagon Background Pattern
        HoneycombBackgroundCanvas(
            modifier = Modifier.fillMaxSize()
        )

        // Subtle vignette gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF0030508)),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // TOP BAR: Logo (Left/Center) + Utility Icons (Right)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Online Indicator + Squircle Merlin TV Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rounded Squircle Merlin TV Logo Emblem
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = AccentSky)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF00B4D8), Color(0xFF0077B6), Color(0xFF03045E))
                                )
                            )
                            .border(1.5.dp, Color(0xFFCAF0F8), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Merlin TV",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MERLIN TV",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                        }
                        Text(
                            text = if (isOnline) "ONLINE • HIGH SPEED" else "OFFLINE MODE",
                            color = if (isOnline) AccentSky else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Right: Action Icons Bar (Search, Timer, REC, Sports, VPN, MSG, UPDATE)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search
                    TopActionIconButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        onClick = onOpenSearch
                    )

                    // Clock / Timer
                    TopActionIconButton(
                        icon = Icons.Default.AccessTime,
                        label = if (currentTime.isNotBlank()) currentTime else "Time",
                        onClick = onOpenCatchUp
                    )

                    // REC / Catch Up
                    TopActionIconButton(
                        icon = Icons.Default.Videocam,
                        label = "REC",
                        onClick = onOpenCatchUp
                    )

                    // Sports
                    TopActionIconButton(
                        icon = Icons.Default.SportsSoccer,
                        label = "Sports",
                        onClick = onOpenSports
                    )

                    // VPN / Status
                    TopActionIconButton(
                        icon = Icons.Default.VpnKey,
                        label = "VPN",
                        onClick = onOpenAccount
                    )

                    // Messages / Notices
                    TopActionIconButton(
                        icon = Icons.Default.Email,
                        label = "MSG",
                        onClick = onOpenNotices
                    )

                    // Update button (highlights with bright badge if available)
                    if (availableUpdate != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(LiveBadgeColor)
                                .border(1.2.dp, Color.White, RoundedCornerShape(8.dp))
                                .clickable { onOpenUpdate() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = Color.White, modifier = Modifier.size(15.dp))
                                Text("UPDATE v${availableUpdate.version}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else {
                        TopActionIconButton(
                            icon = Icons.Default.SystemUpdate,
                            label = "UPDATE",
                            onClick = onOpenUpdate
                        )
                    }
                }
            }

            // ==========================================
            // CENTER: 4 MAIN ROUNDED SQUARE (SQUIRCLE) HUBS
            // LIVE TV | EPG | VOD | SERIES
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LIVE TV
                SquircleHubCard(
                    title = "LIVE TV",
                    subtitle = "1,500+ Live Channels",
                    icon = Icons.Default.LiveTv,
                    accentColor = Color(0xFF00B4D8),
                    onClick = { onOpenSection(NavSection.LIVE) }
                )

                // 2. EPG
                SquircleHubCard(
                    title = "EPG GUIDE",
                    subtitle = "Electronic Schedule",
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    accentColor = Color(0xFF10B981),
                    onClick = { onOpenSection(NavSection.EPG) }
                )

                // 3. VOD (Movies)
                SquircleHubCard(
                    title = "VOD MOVIES",
                    subtitle = "Feature Films",
                    icon = Icons.Default.MovieCreation,
                    accentColor = Color(0xFFF59E0B),
                    onClick = { onOpenSection(NavSection.MOVIES) }
                )

                // 4. SERIES
                SquircleHubCard(
                    title = "TV SERIES",
                    subtitle = "Boxsets & Episodes",
                    icon = Icons.Default.VideoLibrary,
                    accentColor = Color(0xFF8B5CF6),
                    onClick = { onOpenSection(NavSection.SERIES) }
                )
            }

            // ==========================================
            // BOTTOM BAR: Rounded Rectangle Pill Buttons
            // [ACCOUNT] [MULTI] [CATCH UP] ... [FAVORITE] [RADIO] [SETTINGS]
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom Left: ACCOUNT, MULTI, CATCH UP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPillButton(label = "ACCOUNT", icon = Icons.Default.AccountCircle, onClick = onOpenAccount)
                    BottomPillButton(label = "MULTI", icon = Icons.Default.GridView, onClick = { onOpenSection(NavSection.LIVE) })
                    BottomPillButton(label = "CATCH UP", icon = Icons.Default.History, onClick = onOpenCatchUp)
                }

                // Bottom Right: FAVORITE, RADIO, SETTINGS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPillButton(label = "FAVORITE", icon = Icons.Default.Favorite, onClick = { onOpenSection(NavSection.FAVORITES) })
                    BottomPillButton(label = "RADIO", icon = Icons.Default.Radio, onClick = { onOpenSection(NavSection.RADIO) })
                    BottomPillButton(label = "SETTINGS", icon = Icons.Default.Settings, onClick = onOpenSettings)
                }
            }
        }
    }
}

/**
 * High-end Rounded Square (Squircle) Hub Card with D-Pad focus animation, glowing borders, and subtitle.
 */
@Composable
fun SquircleHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "squircleScale")
    val borderColor = if (isFocused) accentColor else Color(0x4464748B)
    val borderWidth = if (isFocused) 3.dp else 1.5.dp
    val shape = RoundedCornerShape(20.dp)

    val bgBrush = if (isFocused) {
        Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.35f),
                Color(0x331E293B),
                Color(0x550F172A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x221E293B),
                Color(0x160F172A),
                Color(0x0A020617)
            )
        )
    }

    Box(
        modifier = modifier
            .width(185.dp)
            .height(175.dp)
            .scale(scale)
            .shadow(if (isFocused) 22.dp else 4.dp, shape, spotColor = accentColor)
            .clip(shape)
            .background(bgBrush)
            .border(borderWidth, borderColor, shape)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isFocused) {
                            Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f)))
                        } else {
                            Brush.linearGradient(listOf(Color(0x33334155), Color(0x221E293B)))
                        }
                    )
                    .border(1.dp, if (isFocused) Color.White.copy(alpha = 0.6f) else Color(0x3394A3B8), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isFocused) Color.White else Color(0xFFE2E8F0),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = if (isFocused) Color.White else Color(0xFFF1F5F9),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = if (isFocused) accentColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Top action icon button with label
 */
@Composable
fun TopActionIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) PrimaryBlue else Color(0x221E293B))
            .border(1.dp, if (isFocused) AccentSky else Color(0x44475569), RoundedCornerShape(8.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.White else Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
            if (label.length <= 8) {
                Text(
                    text = label,
                    color = if (isFocused) Color.White else Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Bottom outline rounded rectangle pill button with icon (e.g. ACCOUNT, MULTI, CATCH UP, FAVORITE, RADIO, SETTINGS)
 */
@Composable
fun BottomPillButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "pillScale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) PrimaryBlue else Color(0x220F172A))
            .border(1.2.dp, if (isFocused) AccentSky else Color(0x66475569), RoundedCornerShape(10.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * Procedural Honeycomb / Hexagon Grid Canvas Pattern
 */
@Composable
fun HoneycombBackgroundCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val hexRadius = 40f
        val hexWidth = hexRadius * kotlin.math.sqrt(3f)
        val hexHeight = hexRadius * 2f
        val vertSpacing = hexHeight * 0.75f
        val horizSpacing = hexWidth

        val rows = (size.height / vertSpacing).toInt() + 2
        val cols = (size.width / horizSpacing).toInt() + 2

        val strokePaint = Stroke(width = 1.2f)
        val hexColor = Color(0x18334155)

        for (r in 0 until rows) {
            val y = r * vertSpacing
            val xOffset = if (r % 2 == 1) horizSpacing / 2f else 0f
            for (c in 0 until cols) {
                val x = c * horizSpacing + xOffset
                drawHexagon(center = Offset(x, y), radius = hexRadius - 2f, color = hexColor, stroke = strokePaint)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Stroke
) {
    val path = Path()
    for (i in 0 until 6) {
        val angleRad = (60f * i - 30f) * (Math.PI / 180f).toFloat()
        val px = center.x + radius * cos(angleRad)
        val py = center.y + radius * sin(angleRad)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path = path, color = color, style = stroke)
}
