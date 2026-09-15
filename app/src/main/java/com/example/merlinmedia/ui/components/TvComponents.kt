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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HeroPreviewPanel(
    item: MediaEntry?,
    channelNumber: Int,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Logo / Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1218))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = item.title.take(3).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentSky
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.type == Kind.LIVE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LiveBadgeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E2433))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1080p 60fps", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val subtitle = listOf(
                    if (channelNumber > 0) "Ch. $channelNumber" else null,
                    item.country.ifBlank { null },
                    item.group.ifBlank { null },
                    item.source.ifBlank { null }
                ).filterNotNull().joinToString("  ·  ")

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentSky,
                    fontWeight = FontWeight.Medium
                )

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onWatchClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Watch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

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
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) FocusRingColor else BorderSubtle,
        animationSpec = tween(durationMillis = 150),
        label = "cardBorder"
    )
    val backgroundColor = if (isFocused) CardSurfaceFocused else CardSurface

    Card(
        modifier = modifier
            .scale(scale)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Channel Number & Favorite Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (channelNumber > 0) "$channelNumber" else "",
                    color = if (isFocused) AccentSky else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onToggleFavorite(item) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentGold else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Logo Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1218))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = item.title.take(3).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentSky
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Channel Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isFocused) AccentSky else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Tags row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.group.ifBlank { item.country },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.type == Kind.LIVE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(LiveBadgeColor)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        modifier = modifier,
        placeholder = { Text(placeholderText, color = TextMuted, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
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