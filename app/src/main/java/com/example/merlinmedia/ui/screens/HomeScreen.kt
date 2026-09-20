package com.example.merlinmedia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    plutoChannels: List<MediaEntry>,
    skyChannels: List<MediaEntry>,
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
        val formatter = java.text.SimpleDateFormat("hh:mm a • EEE, dd MMM", java.util.Locale.getDefault())
        while (true) {
            currentTime = formatter.format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    fun refreshFavorites() {
        favoriteIds.value = favoritesManager.getFavoriteIds()
    }

    val countriesList = listOf(
        "UK" to "🇬🇧 UK Channels",
        "USA" to "🇺🇸 USA Channels",
        "Canada" to "🇨🇦 Canada",
        "Australia" to "🇦🇺 Australia",
        "France" to "🇫🇷 France",
        "Germany" to "🇩🇪 Germany",
        "Spain" to "🇪🇸 Spain",
        "Italy" to "🇮🇹 Italy"
    )

    // Accurate Country Counts (Exact match to prevent count drift)
    val countryCounts = remember(liveChannels) {
        countriesList.associate { (code, _) ->
            val count = liveChannels.count { item ->
                item.country.equals(code, ignoreCase = true) ||
                item.group.startsWith("$code |", ignoreCase = true)
            }
            code to count
        }
    }

    // Precise Filter Logic (Matches Sidebar numbers 100% identically)
    val currentItems = remember(selectedTab, selectedFilter, searchQuery, liveChannels, plutoChannels, skyChannels, movieChannels, seriesChannels, favoriteIds.value) {
        val baseList = when (selectedTab) {
            Kind.LIVE -> liveChannels
            Kind.PLUTO -> plutoChannels
            Kind.SKY -> skyChannels
            Kind.MOVIE -> movieChannels
            Kind.SERIES -> seriesChannels
            Kind.FAVORITES -> {
                val favSet = favoriteIds.value
                val allKnown = liveChannels + plutoChannels + skyChannels + movieChannels + seriesChannels
                allKnown.filter { favSet.contains(it.id) }
            }
        }

        val filteredByFilter = if (selectedFilter == "All") {
            baseList
        } else {
            val isCountry = countriesList.any { it.first.equals(selectedFilter, ignoreCase = true) }
            if (isCountry) {
                baseList.filter { item ->
                    item.country.equals(selectedFilter, ignoreCase = true) ||
                    item.group.startsWith("$selectedFilter |", ignoreCase = true)
                }
            } else {
                baseList.filter { item ->
                    item.group.contains(selectedFilter, ignoreCase = true) ||
                    item.group.equals(selectedFilter, ignoreCase = true)
                }
            }
        }

        if (searchQuery.isBlank()) {
            filteredByFilter
        } else {
            val q = searchQuery.trim().lowercase()
            filteredByFilter.filter { item ->
                item.title.lowercase().contains(q) ||
                item.group.lowercase().contains(q) ||
                item.country.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            }
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
        // Left Navigation Rail (Solid, Clean Sidebar with dynamic country & category list)
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(NavRailBg)
                .border(1.dp, BorderSubtle)
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // App Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
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

            Spacer(modifier = Modifier.height(2.dp))

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
                icon = Icons.Default.Language,
                label = "Pluto TV (${plutoChannels.size})",
                isSelected = selectedTab == Kind.PLUTO && selectedFilter == "All",
                onClick = {
                    selectedTab = Kind.PLUTO
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Sensors,
                label = "Sky & News (${skyChannels.size})",
                isSelected = selectedTab == Kind.SKY && selectedFilter == "All",
                onClick = {
                    selectedTab = Kind.SKY
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.PlayCircle,
                label = "Movies (${movieChannels.size})",
                isSelected = selectedTab == Kind.MOVIE && selectedFilter == "All",
                onClick = {
                    selectedTab = Kind.MOVIE
                    selectedFilter = "All"
                }
            )

            NavRailItem(
                icon = Icons.Default.Movie,
                label = "Series (${seriesChannels.size})",
                isSelected = selectedTab == Kind.SERIES && selectedFilter == "All",
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

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "REGIONS & COUNTRIES",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Country list
            countriesList.forEach { (code, name) ->
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

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "GENRES & CATEGORIES",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
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
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "No internet connection — cached channels available",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Top Bar: Live Clock + Active Filter Badge + Check Updates Button + Search Bar + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Clock & Active Category Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2433))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (currentTime.isNotBlank()) currentTime else "Merlin TV",
                            color = AccentSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val filterLabel = if (selectedFilter != "All") {
                        val cName = countriesList.find { it.first == selectedFilter }?.second ?: selectedFilter
                        "$cName · ${currentItems.size} items"
                    } else {
                        when (selectedTab) {
                            Kind.LIVE -> "All Live TV · ${currentItems.size}"
                            Kind.PLUTO -> "Pluto TV Channels · ${currentItems.size}"
                            Kind.SKY -> "Sky & News Network · ${currentItems.size}"
                            Kind.MOVIE -> "All Movies · ${currentItems.size}"
                            Kind.SERIES -> "All Series · ${currentItems.size}"
                            Kind.FAVORITES -> "Favorites · ${currentItems.size}"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryBlue.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryBlue.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = filterLabel,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Software Update Button
                    Button(
                        onClick = onOpenUpdateDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (availableUpdate != null) LiveBadgeColor else Color(0xFF1E2433)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (availableUpdate != null) Icons.Default.SystemUpdate else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (availableUpdate != null) Color.White else AccentSky,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (availableUpdate != null) "Update v${availableUpdate.version}" else "v${BuildConfig.VERSION_NAME}",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }

                    TvSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search ${currentItems.size} items...",
                        modifier = Modifier.width(220.dp)
                    )

                    IconButton(
                        onClick = onRefreshChannels,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Compact Focused Channel Header Bar (Sleek & Space-Efficient)
            FocusedChannelHeaderBar(
                item = focusedItem,
                channelNumber = focusedIndex,
                onWatchClick = {
                    focusedItem?.let { onSelectChannel(it, currentItems) }
                }
            )

            // Channels Grid (5 Columns, Clean Wide 16:9 Tiles)
            if (isLoading && currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = AccentSky, modifier = Modifier.size(36.dp))
                        Text("Loading channel catalogs...", color = TextSecondary, fontSize = 13.sp)
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TvOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Text("No channels found in this section", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Try selecting 'All' or adjusting your search query.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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