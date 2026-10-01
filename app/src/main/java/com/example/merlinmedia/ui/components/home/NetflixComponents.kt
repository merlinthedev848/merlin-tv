package com.example.merlinmedia.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.ui.theme.*

/**
 * Netflix-Style Hero Spotlight Billboard matching the mockup layout.
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
            .height(300.dp)
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
                modifier = Modifier.fillMaxWidth(0.72f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Tag: "TOP 10 #1 IN MOVIES TODAY"
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
                        text = "#1 IN MOVIES TODAY",
                        color = Color(0xFFFFE066),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bold Cinematic Title (e.g. THE ALCHEMIST'S LEGACY)
                Text(
                    text = item.title.uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Row: Quality Pill, Rating, Year, Genre Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quality badge (4K ULTRA HD / 1080p)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF262626))
                            .border(0.8.dp, Color(0xFF888888), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (item.quality.isNotBlank()) item.quality else "4K ULTRA HD",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // IMDb Rating badge (Yellow pill)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(AccentGold)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "IMDb ${if (item.rating.isNotBlank()) item.rating else "8.9"}",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
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
                    text = if (item.description.isNotBlank()) item.description else "A legendary wizard hunts a dangerous dark magic across centuries to protect a hidden world.",
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
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .scale(if (isPlayFocused) 1.06f else 1.0f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlayFocused) Color.White else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        color = if (isPlayFocused) Color.White else Color.Black,
                        fontSize = 14.sp,
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
                        color = if (isInfoFocused) Color.White else Color(0x66FFFFFF)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .scale(if (isInfoFocused) 1.06f else 1.0f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More Info",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.type == Kind.SERIES) "Episodes & Info" else "More Info",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // My List / Favorite Button
                IconButton(
                    onClick = onToggleFavorite,
                    interactionSource = favInteractionSource,
                    modifier = Modifier
                        .size(40.dp)
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
 * Netflix Top 10 Poster Card with Giant Stylized Red Neon Outline Rank Number (1 through 10)
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
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Giant Stylized Red Neon Outline Rank Number (Matching Mockup exactly)
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(175.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 145f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 10f
                    color = android.graphics.Color.parseColor(if (isFocused) "#FF001E" else "#E50914")
                }

                val fillPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 145f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                    style = android.graphics.Paint.Style.FILL
                    color = android.graphics.Color.parseColor("#141414")
                }

                val text = rank.toString()
                val textBounds = android.graphics.Rect()
                strokePaint.getTextBounds(text, 0, text.length, textBounds)

                val x = (size.width - textBounds.width()) / 2f
                val y = (size.height + textBounds.height()) / 2f

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(text, x, y, fillPaint)
                    canvas.nativeCanvas.drawText(text, x, y, strokePaint)
                }
            }
        }

        // 2:3 Vertical Poster Card overlapping the rank number
        Box(
            modifier = Modifier
                .width(125.dp)
                .height(175.dp)
                .offset(x = (-10).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NetflixCard)
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) NetflixRed else Color(0x44E50914),
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
 * Netflix Horizontal Content Row with Neon Glow Borders for Movies & Shows
 */
@Composable
fun NetflixContentRow(
    title: String,
    icon: ImageVector,
    items: List<MediaEntry>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelectItem: (MediaEntry) -> Unit,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row Header Title
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NetflixRed,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontal Poster Rail
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { index, it -> "rail-${it.id}-$index" }) { index, item ->
                // Distinct glowing neon colors per title (Matrix Green, Dune Gold, Avengers Purple, Blade Runner Red, Oppenheimer Orange, etc.)
                val neonGlow = remember(index, item.title) {
                    when {
                        item.title.contains("Matrix", ignoreCase = true) -> Color(0xFF00E676)
                        item.title.contains("Dune", ignoreCase = true) -> Color(0xFFFFB800)
                        item.title.contains("Avengers", ignoreCase = true) -> Color(0xFFB388FF)
                        item.title.contains("Blade Runner", ignoreCase = true) -> Color(0xFFFF1744)
                        item.title.contains("Oppenheimer", ignoreCase = true) -> Color(0xFFFF9100)
                        item.title.contains("Interstellar", ignoreCase = true) -> Color(0xFF00E5FF)
                        item.title.contains("Witcher", ignoreCase = true) -> Color(0xFF7C4DFF)
                        else -> {
                            val colors = listOf(Color(0xFFE50914), Color(0xFF00E5FF), Color(0xFFFFB800), Color(0xFF00E676), Color(0xFF9C27B0))
                            colors[index % colors.size]
                        }
                    }
                }

                NetflixPosterCard(
                    item = item,
                    neonGlowColor = neonGlow,
                    isFavorite = favoriteIds.contains(item.id),
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onClick = { onSelectItem(item) }
                )
            }
        }
    }
}

/**
 * Standard Netflix Landscape / Poster Card with Neon Border
 */
@Composable
fun NetflixPosterCard(
    item: MediaEntry,
    neonGlowColor: Color,
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

    Box(
        modifier = modifier
            .width(180.dp)
            .height(115.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(NetflixCard)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) neonGlowColor else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        val imgUrl = item.backdrop ?: item.logo
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
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E1E24), Color(0xFF101014)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == Kind.SERIES) Icons.Default.Tv else Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0x66FFFFFF),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Dark Bottom Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 40f
                    )
                )
        )

        // Quality Badge on Top Right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(0.5.dp, neonGlowColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (item.quality.isNotBlank()) item.quality else "HD",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Details (Title + Group)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtext = if (item.genre.isNotBlank()) item.genre else item.group
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Navigation Tab Pill
 */
@Composable
fun NetflixNavTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val containerColor = when {
        isSelected -> Color.White
        isFocused -> Color(0xFF2E2E38)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> Color.Black
        isFocused -> Color.White
        else -> Color(0xFFC0C4CC)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(
                width = if (isFocused && !isSelected) 2.dp else 0.dp,
                color = if (isFocused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

/**
 * Top Icon Action Button
 */
@Composable
fun NetflixIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF2E2E38) else Color(0x66181820))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) NetflixRed else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Slim Left Navigation Rail (Vertical Dock) matching reference mockup
 */
@Composable
fun NetflixLeftNavRail(
    activeSection: NavSection,
    onOpenSection: (NavSection) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(Color(0xFF08080B))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Nav Rail Icons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Home (Red squircle when active)
            LeftNavRailItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = activeSection == NavSection.HOME,
                onClick = { onOpenSection(NavSection.HOME) }
            )

            // Movies (VOD)
            LeftNavRailItem(
                icon = Icons.Default.Movie,
                label = "Movies",
                isSelected = activeSection == NavSection.MOVIES,
                onClick = { onOpenSection(NavSection.MOVIES) }
            )

            // TV Series
            LeftNavRailItem(
                icon = Icons.Default.Tv,
                label = "TV Series",
                isSelected = activeSection == NavSection.SERIES,
                onClick = { onOpenSection(NavSection.SERIES) }
            )

            // Live TV / Antenna
            LeftNavRailItem(
                icon = Icons.Default.LiveTv,
                label = "Live TV",
                isSelected = activeSection == NavSection.LIVE,
                onClick = { onOpenSection(NavSection.LIVE) }
            )

            // My List / Bookmark
            LeftNavRailItem(
                icon = Icons.Default.Bookmark,
                label = "My List",
                isSelected = activeSection == NavSection.FAVORITES,
                onClick = { onOpenSection(NavSection.FAVORITES) }
            )
        }

        // Bottom Menu / Settings Expander
        LeftNavRailItem(
            icon = Icons.Default.Menu,
            label = "Menu",
            isSelected = false,
            onClick = onOpenSettings
        )
    }
}

/**
 * Item for Left Navigation Rail
 */
@Composable
private fun LeftNavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        label = "navRailScale"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> NetflixRed
                    isFocused -> Color(0xFF282832)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused && !isSelected) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected || isFocused) Color.White else Color(0xFF71717A),
            modifier = Modifier.size(18.dp)
        )
    }
}
