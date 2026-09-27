package com.example.merlinmedia.ui.components.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.theme.*

/**
 * Modern Netflix-Style Streaming Dashboard for Merlin TV.
 */
@Composable
fun NetflixDashboard(
    currentTime: String,
    availableUpdate: UpdateInfo?,
    isOnline: Boolean,
    movieChannels: List<MediaEntry>,
    seriesChannels: List<MediaEntry>,
    liveChannels: List<MediaEntry>,
    plutoChannels: List<MediaEntry>,
    skyChannels: List<MediaEntry>,
    recentHistory: List<MediaEntry>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSports: () -> Unit,
    onOpenCatchUp: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMovieForDetails: (MediaEntry) -> Unit,
    onSelectSeriesForEpisodes: (MediaEntry) -> Unit,
    onSelectChannel: (MediaEntry, List<MediaEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Featured Hero Billboard Items (curated top blockbusters & series)
    val featuredCandidates = remember(movieChannels, seriesChannels) {
        val list = mutableListOf<MediaEntry>()
        if (movieChannels.isNotEmpty()) list.addAll(movieChannels.take(5))
        if (seriesChannels.isNotEmpty()) list.addAll(seriesChannels.take(3))
        if (list.isEmpty()) {
            list.add(
                MediaEntry(
                    id = "hero-default-1",
                    title = "Tears of Steel",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    type = Kind.MOVIE,
                    genre = "Sci-Fi • Action",
                    year = "2025",
                    duration = "1h 48m",
                    rating = "8.9 ★",
                    description = "In a dystopian future, scientists and warriors assemble in Amsterdam to save Earth from a rogue cyborg armada.",
                    isVod = true,
                    quality = "4K Ultra HD",
                    backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80"
                )
            )
        }
        list
    }

    var heroIndex by remember { mutableIntStateOf(0) }
    val currentHeroItem = featuredCandidates.getOrElse(heroIndex % featuredCandidates.size) { featuredCandidates.first() }

    // Auto-cycle hero billboard safely
    LaunchedEffect(featuredCandidates.size) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            if (featuredCandidates.isNotEmpty()) {
                heroIndex = (heroIndex + 1) % featuredCandidates.size
            }
        }
    }

    // Top 10 items (combining top movies and top series)
    val top10Items = remember(movieChannels, seriesChannels) {
        val list = mutableListOf<MediaEntry>()
        val maxLen = maxOf(movieChannels.size, seriesChannels.size)
        for (i in 0 until maxLen) {
            if (i < movieChannels.size) list.add(movieChannels[i])
            if (i < seriesChannels.size) list.add(seriesChannels[i])
            if (list.size >= 10) break
        }
        list.take(10)
    }

    // Action & Sci-Fi list
    val actionSciFiItems = remember(movieChannels) {
        movieChannels.filter {
            it.genre.contains("Action", ignoreCase = true) ||
            it.genre.contains("Sci-Fi", ignoreCase = true) ||
            it.genre.contains("Fantasy", ignoreCase = true) ||
            it.group.contains("Action", ignoreCase = true)
        }.ifEmpty { movieChannels.take(12) }
    }

    // Classic & Drama list
    val classicDramaItems = remember(movieChannels) {
        movieChannels.filter {
            it.genre.contains("Classic", ignoreCase = true) ||
            it.genre.contains("Drama", ignoreCase = true) ||
            it.genre.contains("Horror", ignoreCase = true) ||
            it.genre.contains("Thriller", ignoreCase = true)
        }.ifEmpty { movieChannels.drop(5).take(12) }
    }

    // Live TV Top Picks
    val liveTopPicks = remember(skyChannels, liveChannels, plutoChannels) {
        (skyChannels + liveChannels.take(15) + plutoChannels.take(10)).distinctBy { it.url }
    }

    // User's My List Favorites
    val allCatalogItems = remember(movieChannels, seriesChannels, liveChannels, plutoChannels, skyChannels) {
        movieChannels + seriesChannels + liveChannels + plutoChannels + skyChannels
    }
    val myFavoritesList = remember(favoriteIds, allCatalogItems) {
        allCatalogItems.filter { favoriteIds.contains(it.id) }.distinctBy { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // 1. NETFLIX TOP NAVIGATION BAR
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Netflix-Style Merlin TV Logo & Navigation Links
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Netflix-style Bold Red Logo Emblem
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { onOpenSection(NavSection.HOME) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NetflixRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "MERLIN TV",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }

                // Netflix Nav Links: [ Home | Movies | Series | Live TV | My List | Catch Up ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NetflixNavTab(label = "Home", isSelected = true, onClick = {})
                    NetflixNavTab(label = "Movies (VOD)", isSelected = false, onClick = { onOpenSection(NavSection.MOVIES) })
                    NetflixNavTab(label = "TV Series", isSelected = false, onClick = { onOpenSection(NavSection.SERIES) })
                    NetflixNavTab(label = "Live TV", isSelected = false, onClick = { onOpenSection(NavSection.LIVE) })
                    NetflixNavTab(label = "EPG Guide", isSelected = false, onClick = { onOpenSection(NavSection.EPG) })
                    NetflixNavTab(label = "My List", isSelected = false, onClick = { onOpenSection(NavSection.FAVORITES) })
                }
            }

            // Right: Utility Actions (Search, Clock, Update, Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Action
                NetflixIconButton(icon = Icons.Default.Search, label = "Search", onClick = onOpenSearch)

                // Live Clock Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (currentTime.isNotBlank()) currentTime else "Merlin TV",
                        color = Color(0xFFD4D4D8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Update Action
                if (availableUpdate != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NetflixRed)
                            .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                            .clickable { onOpenUpdate() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("UPDATE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    NetflixIconButton(icon = Icons.Default.SystemUpdate, label = "Update", onClick = onOpenUpdate)
                }

                // Settings
                NetflixIconButton(icon = Icons.Default.Settings, label = "Settings", onClick = onOpenSettings)
            }
        }

        // ==========================================
        // 2. NETFLIX SCROLLABLE STREAMING FEED
        // ==========================================
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Section A: Hero Spotlight Billboard
            item(key = "netflix_hero_billboard") {
                NetflixHeroBillboard(
                    item = currentHeroItem,
                    isFavorite = favoriteIds.contains(currentHeroItem.id),
                    onToggleFavorite = { onToggleFavorite(currentHeroItem.id) },
                    onPlay = {
                        val playlist = if (currentHeroItem.type == Kind.SERIES) seriesChannels else movieChannels
                        onSelectChannel(currentHeroItem, playlist)
                    },
                    onMoreInfo = {
                        if (currentHeroItem.type == Kind.SERIES) {
                            onSelectSeriesForEpisodes(currentHeroItem)
                        } else {
                            onSelectMovieForDetails(currentHeroItem)
                        }
                    }
                )
            }

            // Section B: Continue Watching / History
            if (recentHistory.isNotEmpty()) {
                item(key = "netflix_continue_watching") {
                    NetflixContentRow(
                        title = "🕒 Continue Watching",
                        icon = Icons.Default.History,
                        badgeText = "${recentHistory.size} items",
                        items = recentHistory.take(12),
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { item ->
                            if (item.type == Kind.SERIES) onSelectSeriesForEpisodes(item)
                            else if (item.type == Kind.MOVIE) onSelectMovieForDetails(item)
                            else onSelectChannel(item, recentHistory)
                        }
                    )
                }
            }

            // Section C: Top 10 on Merlin TV Today (Numbered Posters)
            if (top10Items.isNotEmpty()) {
                item(key = "netflix_top_10_row") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Top 10 on Merlin TV Today",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(top10Items, key = { index, it -> "top10-${it.id}-$index" }) { index, item ->
                                NetflixTop10PosterCard(
                                    rank = index + 1,
                                    item = item,
                                    isFavorite = favoriteIds.contains(item.id),
                                    onToggleFavorite = { onToggleFavorite(item.id) },
                                    onClick = {
                                        if (item.type == Kind.SERIES) onSelectSeriesForEpisodes(item)
                                        else onSelectMovieForDetails(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section D: Trending Movies (VOD)
            if (movieChannels.isNotEmpty()) {
                item(key = "netflix_trending_movies") {
                    NetflixContentRow(
                        title = "🍿 Trending Blockbuster Movies",
                        icon = Icons.Default.Movie,
                        badgeText = "Explore All (${movieChannels.size}) >",
                        items = movieChannels.take(15),
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { onSelectMovieForDetails(it) }
                    )
                }
            }

            // Section E: Binge-Worthy TV Series
            if (seriesChannels.isNotEmpty()) {
                item(key = "netflix_binge_series") {
                    NetflixContentRow(
                        title = "📺 Binge-Worthy TV Series",
                        icon = Icons.Default.Tv,
                        badgeText = "Explore All (${seriesChannels.size}) >",
                        items = seriesChannels.take(15),
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { onSelectSeriesForEpisodes(it) }
                    )
                }
            }

            // Section F: Action, Sci-Fi & Thrillers
            if (actionSciFiItems.isNotEmpty()) {
                item(key = "netflix_action_scifi") {
                    NetflixContentRow(
                        title = "⚡ Action, Sci-Fi & Fantasy",
                        icon = Icons.Default.Bolt,
                        items = actionSciFiItems,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { onSelectMovieForDetails(it) }
                    )
                }
            }

            // Section G: Live Broadcast TV & FAST Streams
            if (liveTopPicks.isNotEmpty()) {
                item(key = "netflix_live_picks") {
                    NetflixContentRow(
                        title = "📡 Live TV Channels & Top Broadcasts",
                        icon = Icons.Default.LiveTv,
                        badgeText = "All Channels >",
                        items = liveTopPicks.take(15),
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { onSelectChannel(it, liveTopPicks) }
                    )
                }
            }

            // Section H: Classic Cinema & Drama
            if (classicDramaItems.isNotEmpty()) {
                item(key = "netflix_classic_drama") {
                    NetflixContentRow(
                        title = "🎭 Classic Cinema & Timeless Favourites",
                        icon = Icons.Default.TheaterComedy,
                        items = classicDramaItems,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { onSelectMovieForDetails(it) }
                    )
                }
            }

            // Section I: My Watchlist / Favorites
            if (myFavoritesList.isNotEmpty()) {
                item(key = "netflix_my_favorites") {
                    NetflixContentRow(
                        title = "⭐ My Watchlist",
                        icon = Icons.Default.Star,
                        badgeText = "${myFavoritesList.size} saved",
                        items = myFavoritesList,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onSelectItem = { item ->
                            if (item.type == Kind.SERIES) onSelectSeriesForEpisodes(item)
                            else if (item.type == Kind.MOVIE) onSelectMovieForDetails(item)
                            else onSelectChannel(item, myFavoritesList)
                        }
                    )
                }
            }
        }

        // ==========================================
        // 3. BOTTOM UTILITY BAR (ACCOUNT, MULTI, CATCHUP, RADIO, SETTINGS)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomPillButton(label = "ACCOUNT", icon = Icons.Default.AccountCircle, onClick = onOpenAccount)
                BottomPillButton(label = "CATCH UP", icon = Icons.Default.History, onClick = onOpenCatchUp)
                BottomPillButton(label = "SPORTS", icon = Icons.Default.SportsSoccer, onClick = onOpenSports)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomPillButton(label = "RADIO", icon = Icons.Default.Radio, onClick = { onOpenSection(NavSection.RADIO) })
                BottomPillButton(label = "NOTICES", icon = Icons.Default.Email, onClick = onOpenNotices)
                BottomPillButton(label = "SETTINGS", icon = Icons.Default.Settings, onClick = onOpenSettings)
            }
        }
    }
}

/**
 * Top Navigation Tab Item for Netflix Top Bar
 */
@Composable
private fun NetflixNavTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "navTabScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) NetflixRed else if (isSelected) Color(0xFF22222A) else Color.Transparent)
            .border(
                width = if (isFocused) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) Color.White else if (isSelected) NetflixRed else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isFocused || isSelected) Color.White else Color(0xFFB3B3B3),
            fontSize = 12.5.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Icon button for Netflix Top Bar (Search, Settings, Updates)
 */
@Composable
private fun NetflixIconButton(
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
            .size(34.dp)
            .scale(if (isFocused) 1.15f else 1.0f)
            .clip(CircleShape)
            .background(if (isFocused) NetflixRed else Color(0xFF1E1E24))
            .border(
                width = if (isFocused) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.size(16.dp)
        )
    }
}
