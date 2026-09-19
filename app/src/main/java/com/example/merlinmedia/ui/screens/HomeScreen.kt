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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.merlinmedia.BuildConfig
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

    // Live Clock State
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = java.text.SimpleDateFormat("hh:mm:ss a • EEE, dd MMM", java.util.Locale.getDefault())
        while (true) {
            currentTime = formatter.format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

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

    // Dynamic country counts
    val countryCounts = remember(liveChannels) {
        liveChannels.groupingBy { it.country.ifBlank { "Global" } }.eachCount()
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
        // Left Navigation Rail (Solid, Clean Sidebar with dynamic country & category list)
        Column(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight()
                .background(NavRailBg)
                .border(1.dp, BorderSubtle)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // App Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
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
                Column {
                    Text(
                        text = "Merlin TV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} Premium",
                        fontSize = 10.sp,
                        color = PrimaryCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Tabs
            NavRailItem(
                icon = Icons.Default.LiveTv,
                label = "Live TV (${liveChannels.size})",
                isSelected = selectedTab == Kind.LIVE && selectedFilter == "All",
                onClick = {
                    selectedTab = Kind.LIVE
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.PlayCircle,
                label = "Movies (${movieChannels.size})",
                isSelected = selectedTab == Kind.MOVIE,
                onClick = {
                    selectedTab = Kind.MOVIE
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Movie,
                label = "Series (${seriesChannels.size})",
                isSelected = selectedTab == Kind.SERIES,
                onClick = {
                    selectedTab = Kind.SERIES
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Favorite,
                label = "Favorites (${favoriteIds.value.size})",
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

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "REGIONS & COUNTRIES",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Dynamic country list
            val countries = listOf(
                "UK" to "🇬🇧 UK Channels",
                "USA" to "🇺🇸 USA Channels",
                "Canada" to "🇨🇦 Canada",
                "Australia" to "🇦🇺 Australia",
                "France" to "🇫🇷 France",
                "Germany" to "🇩🇪 Germany",
                "Spain" to "🇪🇸 Spain",
                "Italy" to "🇮🇹 Italy"
            )

            countries.forEach { (code, name) ->
                val count = countryCounts[code] ?: 0
                NavRailItem(
                    icon = Icons.Default.Flag,
                    label = if (count > 0) "$name ($count)" else name,
                    isSelected = selectedTab == Kind.LIVE && selectedFilter == code,
                    onClick = {
                        selectedTab = Kind.LIVE
                        selectedFilter = code
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "GENRES & CATEGORIES",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Category filters
            val categories = listOf(
                "Entertainment" to Icons.Default.TheaterComedy,
                "News" to Icons.AutoMirrored.Filled.Feed,
                "Sports" to Icons.Default.SportsSoccer,
                "Movies" to Icons.Default.MovieCreation,
                "Kids" to Icons.Default.ChildCare,
                "Music" to Icons.Default.MusicNote,
                "Science" to Icons.Default.Science,
                "Documentary" to Icons.AutoMirrored.Filled.MenuBook
            )

            categories.forEach { (cat, icon) ->
                NavRailItem(
                    icon = icon,
                    label = cat,
                    isSelected = selectedTab == Kind.LIVE && selectedFilter.equals(cat, ignoreCase = true),
                    onClick = {
                        selectedTab = Kind.LIVE
                        selectedFilter = cat
                    }
                )
            }
        }

        // Right Main Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Offline banner
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

            // Top Bar: Live Clock + Check Updates Button + Search Bar + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Clock & Active Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2433))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentTime.isNotBlank()) currentTime else "Merlin TV",
                            color = AccentSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedFilter != "All") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryBlue.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryBlue, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Filtered: $selectedFilter (${currentItems.size})",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Software Update Button
                    Button(
                        onClick = onOpenUpdateDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (availableUpdate != null) LiveBadgeColor else Color(0xFF1E2433)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (availableUpdate != null) Icons.Default.SystemUpdate else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (availableUpdate != null) Color.White else AccentSky,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (availableUpdate != null) "Update v${availableUpdate.version}" else "v${BuildConfig.VERSION_NAME} Updates",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    TvSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search ${currentItems.size} items...",
                        modifier = Modifier.width(260.dp)
                    )

                    IconButton(
                        onClick = onRefreshChannels,
                        modifier = Modifier
                            .size(36.dp)
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

            // Channels Grid
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
                        Text("Try selecting 'All' or adjusting your search.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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