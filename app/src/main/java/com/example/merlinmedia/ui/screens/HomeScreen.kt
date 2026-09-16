package com.example.merlinmedia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.data.CatalogRepository
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.components.*
import com.example.merlinmedia.ui.theme.*

@Composable
fun HomeScreen(
    liveChannels: List<MediaEntry>,
    movieChannels: List<MediaEntry>,
    seriesChannels: List<MediaEntry>,
    isLoading: Boolean,
    isOnline: Boolean,
    availableUpdate: UpdateInfo?,
    favoritesManager: FavoritesManager,
    onSelectChannel: (item: MediaEntry, playlist: List<MediaEntry>) -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    onRefreshChannels: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Kind.LIVE) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val favoriteIds = remember { mutableStateOf(favoritesManager.getFavoriteIds()) }
    var focusedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var focusedIndex by remember { mutableIntStateOf(101) }

    fun refreshFavorites() {
        favoriteIds.value = favoritesManager.getFavoriteIds()
    }

    // Filter items based on active tab, filter, and search query
    val currentItems = remember(selectedTab, selectedFilter, searchQuery, liveChannels, movieChannels, seriesChannels, favoriteIds.value) {
        val baseList = when (selectedTab) {
            Kind.LIVE -> liveChannels
            Kind.MOVIE -> movieChannels
            Kind.SERIES -> seriesChannels
            Kind.FAVORITES -> {
                // Show only actual favourites from loaded catalogs
                val favSet = favoriteIds.value
                val allKnown = liveChannels + movieChannels + seriesChannels
                allKnown.filter { favSet.contains(it.id) }
            }
        }

        baseList.filter { item ->
            val matchesFilter = if (selectedFilter != "All") {
                item.country.equals(selectedFilter, ignoreCase = true) ||
                        item.group.contains(selectedFilter, ignoreCase = true) ||
                        item.source.contains(selectedFilter, ignoreCase = true)
            } else {
                true
            }

            val matchesSearch = if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) ||
                        item.group.lowercase().contains(q) ||
                        item.country.lowercase().contains(q) ||
                        item.description.lowercase().contains(q)
            } else {
                true
            }

            matchesFilter && matchesSearch
        }
    }

    // Keep focusedItem and its channel number in sync with filtered list
    LaunchedEffect(currentItems) {
        if (currentItems.isNotEmpty() && (focusedItem == null || !currentItems.contains(focusedItem))) {
            focusedItem = currentItems.first()
            focusedIndex = 101
        } else if (focusedItem != null) {
            val idx = currentItems.indexOf(focusedItem)
            if (idx >= 0) focusedIndex = 101 + idx
        }
    }


    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Left Navigation Rail (Solid, Clean Sidebar)
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(NavRailBg)
                .border(1.dp, BorderSubtle)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // App Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Merlin TV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Tabs
            NavRailItem(
                icon = Icons.Default.LiveTv,
                label = "Live TV",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "All",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.PlayCircle,
                label = "Movies",
                isSelected = selectedTab == Kind.MOVIE,
                onClick = {
                    selectedTab = Kind.MOVIE
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Movie,
                label = "Series",
                isSelected = selectedTab == Kind.SERIES,
                onClick = {
                    selectedTab = Kind.SERIES
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Favorite,
                label = "Favorites",
                isSelected = selectedTab == Kind.FAVORITES,
                onClick = {
                    selectedTab = Kind.FAVORITES
                    selectedFilter = "All"
                    refreshFavorites()
                }
            )

            NavRailItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = false,
                onClick = onOpenSettingsDialog
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "CHANNELS & REGIONS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category & Region Filters
            NavRailItem(
                icon = Icons.Default.Flag,
                label = "UK Channels",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "UK",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "UK"
                }
            )

            NavRailItem(
                icon = Icons.Default.Flag,
                label = "USA Channels",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "USA",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "USA"
                }
            )

            NavRailItem(
                icon = Icons.Default.SportsSoccer,
                label = "Sports",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "Sports",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "Sports"
                }
            )

            NavRailItem(
                icon = Icons.AutoMirrored.Filled.Feed,
                label = "News",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "News",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "News"
                }
            )

            NavRailItem(
                icon = Icons.Default.TheaterComedy,
                label = "Entertainment",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "Entertainment",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "Entertainment"
                }
            )
        }

        // Right Main Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fix #14: Offline banner — was previously received but never displayed
            AnimatedVisibility(
                visible = !isOnline,
                enter = androidx.compose.animation.slideInVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically() + androidx.compose.animation.fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB91C1C))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "No internet connection — live channels unavailable",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Top Bar: Check Updates Button + Search Bar + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Software Update Button
                Button(
                    onClick = onOpenUpdateDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (availableUpdate != null) LiveBadgeColor else Color(0xFF1E2433)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (availableUpdate != null) Icons.Default.SystemUpdate else Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (availableUpdate != null) Color.White else AccentSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (availableUpdate != null) "Update v${availableUpdate.version}" else "Check Updates",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search channels...",
                        modifier = Modifier.width(280.dp)
                    )

                    IconButton(
                        onClick = onRefreshChannels,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Hero Live Preview Panel
            HeroPreviewPanel(
                item = focusedItem,
                channelNumber = focusedIndex,
                onWatchClick = {
                    focusedItem?.let { onSelectChannel(it, currentItems) }
                }
            )

            // Channels Grid (Shelves)
            if (isLoading && currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AccentSky)
                        Text("Loading channel catalogs...", color = TextSecondary)
                    }
                }
            } else if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TvOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Text("No channels found in this section", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = currentItems,
                        key = { _, it -> "${it.id}-${it.url}" }
                    ) { index, item ->
                        val chNum = 101 + index
                        ChannelGridCard(
                            item = item,
                            channelNumber = chNum,
                            isFavorite = favoriteIds.value.contains(item.id),
                            onToggleFavorite = {
                                favoritesManager.toggleFavorite(item.id)
                                refreshFavorites()
                            },
                            onFocusChange = {
                                focusedItem = item
                                focusedIndex = chNum
                            },
                            onClick = {
                                onSelectChannel(item, currentItems)
                            }
                        )
                    }
                }
            }
        }
    }
}