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
 * High-end Classic Honeycomb Dashboard matching the clean IPTV TV-Box experience.
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
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // TOP BAR: Logo (Center) + Utility Icons (Right)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer / clock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Text(
                        text = if (isOnline) "ONLINE" else "OFFLINE",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Center: Circular Merlin TV Logo Emblem
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(16.dp, CircleShape, spotColor = AccentSky)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00B4D8), Color(0xFF0077B6), Color(0xFF90E0EF))
                            )
                        )
                        .border(2.dp, Color(0xFFCAF0F8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Modern play arrow symbol
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Merlin TV",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Right: Action Icons Bar (Search, Timer, REC, Sports, VPN, MSG, UPDATE)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(LiveBadgeColor)
                                .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                                .clickable { onOpenUpdate() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("UPDATE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
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
            // CENTER: 4 MAIN CIRCULAR HUBS
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
                CircularHubCard(
                    title = "LIVE TV",
                    icon = Icons.Default.Tv,
                    onClick = { onOpenSection(NavSection.LIVE) }
                )

                // 2. EPG
                CircularHubCard(
                    title = "EPG",
                    icon = Icons.Default.FormatListBulleted,
                    onClick = { onOpenSection(NavSection.EPG) }
                )

                // 3. VOD (Movies)
                CircularHubCard(
                    title = "VOD",
                    icon = Icons.Default.MovieCreation,
                    onClick = { onOpenSection(NavSection.MOVIES) }
                )

                // 4. SERIES
                CircularHubCard(
                    title = "SERIES",
                    icon = Icons.Default.VideoLibrary,
                    onClick = { onOpenSection(NavSection.SERIES) }
                )
            }

            // ==========================================
            // BOTTOM BAR: Pill Buttons
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
                    BottomPillButton(label = "ACCOUNT", onClick = onOpenAccount)
                    BottomPillButton(label = "MULTI", onClick = { onOpenSection(NavSection.LIVE) })
                    BottomPillButton(label = "CATCH UP", onClick = onOpenCatchUp)
                }

                // Bottom Right: FAVORITE, RADIO, SETTINGS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPillButton(label = "FAVORITE", onClick = { onOpenSection(NavSection.FAVORITES) })
                    BottomPillButton(label = "RADIO", onClick = { onOpenSection(NavSection.RADIO) })
                    BottomPillButton(label = "SETTINGS", onClick = onOpenSettings)
                }
            }
        }
    }
}

/**
 * Large circular outline hub button with D-Pad focus animation, glowing borders, and icon.
 */
@Composable
fun CircularHubCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.10f else 1.0f, label = "hubScale")
    val borderColor = if (isFocused) AccentSky else Color(0x6694A3B8)
    val borderWidth = if (isFocused) 3.dp else 1.5.dp
    val bgBrush = if (isFocused) {
        Brush.radialGradient(
            colors = listOf(Color(0x5500B4D8), Color(0x330077B6), Color(0x1A090D15))
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0x221E293B), Color(0x110F172A), Color(0x05000000))
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .scale(scale)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .shadow(if (isFocused) 20.dp else 4.dp, CircleShape, spotColor = AccentSky)
                .clip(CircleShape)
                .background(bgBrush)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isFocused) Color.White else Color(0xFFCBD5E1),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    color = if (isFocused) Color.White else Color(0xFFCBD5E1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            }
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
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) PrimaryBlue else Color(0x221E293B))
            .border(1.dp, if (isFocused) AccentSky else Color(0x44475569), RoundedCornerShape(6.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
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
            if (label.length <= 6) {
                Text(
                    text = label,
                    color = if (isFocused) Color.White else Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Bottom outline pill button (e.g. ACCOUNT, MULTI, CATCH UP, FAVORITE, RADIO, SETTINGS)
 */
@Composable
fun BottomPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "pillScale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) PrimaryBlue else Color(0x220F172A))
            .border(1.2.dp, if (isFocused) AccentSky else Color(0x66475569), RoundedCornerShape(6.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFocused) Color.White else Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
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
