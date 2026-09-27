package com.example.merlinmedia.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.ui.theme.*

/**
 * Netflix-Style Hero Spotlight Billboard with cinematic backdrop, rating metadata, and D-Pad focusable CTA buttons.
 */
@Composable
fun NetflixHeroBillboard(
    item: MediaEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayFocused by playInteractionSource.collectIsFocusedAsState()

    val infoInteractionSource = remember { MutableInteractionSource() }
    val isInfoFocused by infoInteractionSource.collectIsFocusedAsState()

    val favInteractionSource = remember { MutableInteractionSource() }
    val isFavFocused by favInteractionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F0F12))
            .border(1.dp, Color(0x33E50914), RoundedCornerShape(16.dp))
    ) {
        // High-res Backdrop Image
        val backdropUrl = item.backdrop ?: item.logo
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Cinematic Netflix Multi-Gradient Mask (Dark left & bottom vignette)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFA0B0B0F),
                            Color(0xEE0F0F14),
                            Color(0x880F0F14),
                            Color(0x22000000)
                        ),
                        startX = 0f,
                        endX = 1400f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66000000),
                            Color(0xF00B0B0F)
                        )
                    )
                )
        )

        // Content Details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.68f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Tag: "🔥 #1 ON MERLIN TV TODAY" or "⭐ MERLIN ORIGINAL"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NetflixRed)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TOP 10",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "#1 in Movies & TV Series Today",
                        color = Color(0xFFFFE066),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bold Cinematic Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Row: Quality Pill, Rating, Year, Genre Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quality badge (4K / 1080p)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF262626))
                            .border(0.8.dp, Color(0xFF666666), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (item.quality.isNotBlank()) item.quality else "4K Ultra HD",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Rating badge
                    if (item.rating.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(13.dp))
                            Text(
                                text = item.rating,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Release Year
                    if (item.year.isNotBlank()) {
                        Text(
                            text = item.year,
                            color = Color(0xFFCCCCCC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Duration or Genre
                    val metaLabel = if (item.duration.isNotBlank()) item.duration else item.genre
                    if (metaLabel.isNotBlank()) {
                        Text(
                            text = "•  $metaLabel",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Synopsis Summary
                Text(
                    text = if (item.description.isNotBlank()) item.description else "Stream this blockbuster title with full audio and subtitles directly on Merlin TV.",
                    color = Color(0xFFD4D4D8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Remote Focusable Action Buttons: [ ▶ Play ] [ ℹ More Info ] [ ＋ My List ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play Button (Netflix Solid White Button with Black Text)
                Button(
                    onClick = onPlay,
                    interactionSource = playInteractionSource,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayFocused) NetflixRed else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .scale(if (isPlayFocused) 1.06f else 1.0f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlayFocused) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        color = if (isPlayFocused) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // More Info Button (Frosted Dark Glass)
                Button(
                    onClick = onMoreInfo,
                    interactionSource = infoInteractionSource,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInfoFocused) Color(0xFF33333E) else Color(0x662B2B36)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isInfoFocused) 2.dp else 1.dp,
                        color = if (isInfoFocused) Color.White else Color(0x44FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .scale(if (isInfoFocused) 1.06f else 1.0f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More Info",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.type == Kind.SERIES) "Episodes & Info" else "More Info",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // My List / Favorite Button
                IconButton(
                    onClick = onToggleFavorite,
                    interactionSource = favInteractionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .scale(if (isFavFocused) 1.12f else 1.0f)
                        .clip(CircleShape)
                        .background(if (isFavFocused) Color(0xFF33333E) else Color(0x662B2B36))
                        .border(
                            width = if (isFavFocused) 2.dp else 1.dp,
                            color = if (isFavFocused) NetflixRed else Color(0x44FFFFFF),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "My List",
                        tint = if (isFavorite) AccentGold else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Netflix Top 10 Poster Card with Giant Stylized Rank Number (1 through 10)
 */
@Composable
fun NetflixTop10PosterCard(
    rank: Int,
    item: MediaEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(120),
        label = "top10Scale"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(200.dp)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Giant Stylized Rank Number
        Box(
            modifier = Modifier
                .width(65.dp)
                .height(180.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "$rank",
                color = if (isFocused) NetflixRed else Color(0xFF4B5563),
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier.offset(x = 10.dp)
            )
        }

        // 2:3 Vertical Poster Card
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(185.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NetflixCard)
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) NetflixRed else Color(0x33475569),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            if (!item.logo.isNullOrBlank()) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == Kind.SERIES) Icons.Default.Tv else Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0x88FFFFFF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Rating / Year Badge on Top
            if (item.rating.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(item.rating, color = AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Gradient Title Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Reusable Horizontal Content Rail (Trending Now, TV Series, Action, Live TV, etc.)
 */
@Composable
fun NetflixContentRow(
    title: String,
    icon: ImageVector? = null,
    badgeText: String? = null,
    items: List<MediaEntry>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelectItem: (MediaEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row Title & Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            if (badgeText != null) {
                Text(
                    text = badgeText,
                    color = AccentSky,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontal Carousel of 2:3 Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { index, it -> "netflix-row-${it.id}-$index" }) { _, item ->
                NetflixPosterCard(
                    item = item,
                    isFavorite = favoriteIds.contains(item.id),
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onClick = { onSelectItem(item) }
                )
            }
        }
    }
}

/**
 * Modern 2:3 Vertical Netflix Squircle Poster Card with Smooth D-Pad Lift & Glow
 */
@Composable
fun NetflixPosterCard(
    item: MediaEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(120),
        label = "posterScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(135.dp)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .shadow(if (isFocused) 16.dp else 2.dp, RoundedCornerShape(10.dp), spotColor = NetflixRed)
                .clip(RoundedCornerShape(10.dp))
                .background(NetflixCard)
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) NetflixRed else Color(0x33475569),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            val imgUrl = item.logo ?: item.backdrop
            if (!imgUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == Kind.SERIES) Icons.Default.Tv else (if (item.isVod) Icons.Default.Movie else Icons.Default.LiveTv),
                        contentDescription = null,
                        tint = Color(0x88FFFFFF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Top Badges: Quality / Live / Rating
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.type == Kind.LIVE || item.type == Kind.PLUTO || item.type == Kind.SKY) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NetflixRed)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                } else if (item.year.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(item.year, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (item.rating.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(item.rating, color = AccentGold, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Bottom Gradient Label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (item.duration.isNotBlank()) item.duration else (if (item.genre.isNotBlank()) item.genre else (if (item.group.isNotBlank()) item.group else "Merlin TV")),
                    color = Color(0xFFD4D4D8),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Title below poster
        Text(
            text = item.title,
            color = if (isFocused) Color.White else Color(0xFFE2E8F0),
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
