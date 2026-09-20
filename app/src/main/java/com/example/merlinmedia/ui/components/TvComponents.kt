package com.example.merlinmedia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.ui.theme.*

@Composable
fun NavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused -> CardSurfaceFocused
        isSelected -> Color(0xFF1E2433)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> AccentSky
        isSelected -> TextPrimary
        else -> TextSecondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) FocusRingColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
    }
}

/**
 * Sleek, compact Header Details Bar with Cobra/TiviMate style EPG 'Now & Next' timeline.
 * Takes up minimal vertical space (~64dp) leaving maximum room for channel browsing.
 */
@Composable
fun FocusedChannelHeaderBar(
    item: MediaEntry?,
    channelNumber: Int,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    // Derive realistic simulated EPG air time & program details based on current clock & channel name
    val calendar = java.util.Calendar.getInstance()
    val minuteOfHour = calendar.get(java.util.Calendar.MINUTE)
    val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val progress = (minuteOfHour / 60f).coerceIn(0.05f, 0.95f)
    val minsLeft = 60 - minuteOfHour
    val startTime = String.format("%02d:00", hourOfDay)
    val endTime = String.format("%02d:00", (hourOfDay + 1) % 24)

    val isFhd = item.title.contains("1080", ignoreCase = true) || item.title.contains("FHD", ignoreCase = true) || item.group.contains("NEWS", ignoreCase = true) || item.group.contains("SPORTS", ignoreCase = true)
    val is4k = item.title.contains("4K", ignoreCase = true) || item.title.contains("UHD", ignoreCase = true)

    val currentProgram = if (item.description.isNotBlank()) item.description else "Live Broadcast: ${item.title}"
    val nextProgram = "Up Next ($endTime): Featured ${if (item.group.isNotBlank()) item.group.substringAfter("|").trim() else "Programming"}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Channel Logo or Initial Badge + Info Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Compact Logo / Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = item.logo,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = item.title.take(3).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentSky,
                            fontSize = 13.sp
                        )
                    }
                }

                // Channel Info Title, Tags, and EPG Progress Bar
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ch. $channelNumber",
                            color = AccentSky,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (item.type == Kind.LIVE || item.type == Kind.PLUTO || item.type == Kind.SKY) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LiveBadgeColor)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Resolution Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (is4k) Quality4kBadge else if (isFhd) QualityFhdBadge else Color(0xFF334155))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (is4k) "4K UHD" else if (isFhd) "1080p FHD" else "HD 720p",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (item.country.isNotBlank()) {
                            Text(
                                text = "· ${item.country}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // EPG Now Playing Line & Timeline Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            text = "$startTime - $endTime",
                            color = AccentSky,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Air Time Progress Bar
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(EpgProgressTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(EpgProgressFill)
                            )
                        }

                        Text(
                            text = "${minsLeft}m left  ·  $currentProgram",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Watch Fullscreen Button
            Button(
                onClick = onWatchClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Watch Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Modern Wide 16:9 Channel Tile.
 * Optimized for Android TV D-Pad navigation:
 * - Shows clear channel number + favorite star
 * - Center channel brand logo (with sleek fallback badge, never an empty void)
 * - Channel title and non-wrapping LIVE badge
 */
@Composable
fun ChannelGridCard(
    item: MediaEntry,
    channelNumber: Int,
    isFavorite: Boolean,
    onToggleFavorite: (MediaEntry) -> Unit,
    onFocusChange: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusChange()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) FocusRingColor else BorderSubtle,
        animationSpec = tween(durationMillis = 120),
        label = "cardBorder"
    )
    val backgroundColor = if (isFocused) CardSurfaceFocused else CardSurface

    Card(
        modifier = modifier
            .scale(scale)
            .height(108.dp)
            .border(if (isFocused) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Channel Number & Favorite Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) PrimaryBlue else Color(0xFF0F141E))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$channelNumber",
                        color = if (isFocused) Color.White else AccentSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(item) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentGold else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Center: Channel Branding / Logo
            val logoBgModifier = if (!item.logo.isNullOrBlank()) {
                Modifier.background(Color(0xFF0C0F17))
            } else {
                Modifier.background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(logoBgModifier),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = AccentSky.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.title.take(8).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Bottom: Title & Live Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isFocused) AccentSky else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.type == Kind.LIVE) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(LiveBadgeColor)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search channels..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(40.dp),
        placeholder = { Text(placeholderText, color = TextMuted, fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FocusRingColor,
            unfocusedBorderColor = BorderSubtle,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

/**
 * Cobra/TiviMate Style Quick Channel Guide Item for in-player overlay drawer.
 */
@Composable
fun QuickChannelDrawerItem(
    item: MediaEntry,
    channelNumber: Int,
    isCurrentPlaying: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused -> CardSurfaceFocused
        isCurrentPlaying -> PrimaryBlue.copy(alpha = 0.25f)
        else -> Color(0xFF131824)
    }

    val borderColor = when {
        isFocused -> FocusRingColor
        isCurrentPlaying -> PrimaryBlue.copy(alpha = 0.6f)
        else -> BorderSubtle
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Channel Number
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isFocused) PrimaryBlue else Color(0xFF0C1019))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$channelNumber",
                    color = if (isFocused) Color.White else AccentSky,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Channel Logo / Initial
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0E131E)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = item.title.take(3).uppercase(),
                        color = AccentSky,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title & Group
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (isFocused || isCurrentPlaying) TextPrimary else Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = if (isFocused || isCurrentPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.group.isNotBlank()) item.group else (if (item.country.isNotBlank()) item.country else "Live"),
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right side indicators: Playing icon / Favorite
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isCurrentPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = AccentSky,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isFavorite) AccentGold else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}