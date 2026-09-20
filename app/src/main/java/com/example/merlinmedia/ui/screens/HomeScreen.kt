package com.example.merlinmedia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

enum class NavSection {
    HOME,
    LIVE,
    PLUTO,
    SKY,
    MOVIES,
    SERIES,
    RADIO,
    HUB,
    FAVORITES
}

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
    var activeSection by remember { mutableStateOf(NavSection.HOME) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    val favoriteIds = remember { mutableStateOf(favoritesManager.getFavoriteIds()) }
    var focusedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var focusedIndex by remember { mutableIntStateOf(101) }

    // Hero Carousel Index
    var heroSlideIndex by remember { mutableIntStateOf(0) }

    // Live Clock State (Format matching CobraTV Pro: 8:57 PM Tue, 25 Aug)
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = java.text.SimpleDateFormat("h:mm a  EEE, dd MMM", java.util.Locale.getDefault())
        while (true) {
            currentTime = formatter.format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // Auto-cycle Hero Carousel every 8 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(8000)
            heroSlideIndex = (heroSlideIndex + 1) % 4
        }
    }

    fun refreshFavorites() {
        favoriteIds.value = favoritesManager.getFavoriteIds()
    }

    val countriesList = listOf(
        "UK" to "🇬🇧 UK",
        "USA" to "🇺🇸 USA",
        "Canada" to "🇨🇦 Canada",
        "Australia" to "🇦🇺 Australia",
        "New Zealand" to "🇳🇿 NZ",
        "Ireland" to "🇮🇪 Ireland",
        "France" to "🇫🇷 France",
        "Germany" to "🇩🇪 Germany",
        "Italy" to "🇮🇹 Italy",
        "Spain" to "🇪🇸 Spain",
        "Portugal" to "🇵🇹 Portugal",
        "Netherlands" to "🇳🇱 Netherlands",
        "South Africa" to "🇿🇦 South Africa",
        "India" to "🇮🇳 India",
        "Japan" to "🇯🇵 Japan",
        "South Korea" to "🇰🇷 Korea",
        "Philippines" to "🇵🇭 Philippines",
        "Brazil" to "🇧🇷 Brazil",
        "Mexico" to "🇲🇽 Mexico"
    )

    // Accurate Country Counts
    val countryCounts = remember(liveChannels) {
        countriesList.associate { (code, _) ->
            val count = liveChannels.count { item ->
                item.country.equals(code, ignoreCase = true) ||
                item.group.startsWith("$code |", ignoreCase = true)
            }
            code to count
        }
    }

    // Dynamic Filter Categories (Full Public FAST & IPTV Taxonomy)
    val filterCategories = listOf(
        "All", "News", "Sports", "Movies", "Series", "Entertainment",
        "Documentary", "Kids", "Animation", "Comedy", "Music", "Cooking",
        "Travel", "Science", "Education", "Business", "Weather", "Classic", "Auto"
    )

    // Filter Logic based on Active Section
    val currentItems = remember(activeSection, selectedFilter, searchQuery, liveChannels, plutoChannels, skyChannels, movieChannels, seriesChannels, favoriteIds.value) {
        val baseList = when (activeSection) {
            NavSection.HOME, NavSection.HUB -> skyChannels + plutoChannels.take(40) + liveChannels.take(40) + movieChannels.take(20)
            NavSection.LIVE -> liveChannels
            NavSection.PLUTO -> plutoChannels
            NavSection.SKY -> skyChannels
            NavSection.MOVIES -> movieChannels
            NavSection.SERIES -> seriesChannels
            NavSection.RADIO -> liveChannels.filter { it.group.contains("music", ignoreCase = true) || it.title.contains("radio", ignoreCase = true) }
            NavSection.FAVORITES -> {
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
                    item.title.contains(selectedFilter, ignoreCase = true)
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

    // Keep focusedItem and its channel number in sync
    LaunchedEffect(currentItems) {
        if (currentItems.isNotEmpty() && (focusedItem == null || !currentItems.contains(focusedItem))) {
            focusedItem = currentItems.first()
            focusedIndex = 101
        } else if (focusedItem != null) {
            val idx = currentItems.indexOf(focusedItem)
            if (idx >= 0) focusedIndex = 101 + idx
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // 1. TOP HORIZONTAL NAVIGATION & ACTION BAR
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding (Merlin TV Pro with Snake/Wizard Cyan Badge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                Column {
                    Text(
                        text = "MerlinTV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "PRO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentSky,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Horizontal Navigation Menu Tabs (CobraTV Pro Style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                TopNavBarItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = activeSection == NavSection.HOME,
                    onClick = { activeSection = NavSection.HOME; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.LiveTv,
                    label = "Live",
                    isSelected = activeSection == NavSection.LIVE,
                    onClick = { activeSection = NavSection.LIVE; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.Language,
                    label = "Pluto",
                    isSelected = activeSection == NavSection.PLUTO,
                    onClick = { activeSection = NavSection.PLUTO; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.Sensors,
                    label = "Sky",
                    isSelected = activeSection == NavSection.SKY,
                    onClick = { activeSection = NavSection.SKY; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.PlayCircle,
                    label = "Movies",
                    isSelected = activeSection == NavSection.MOVIES,
                    onClick = { activeSection = NavSection.MOVIES; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.Movie,
                    label = "Series",
                    isSelected = activeSection == NavSection.SERIES,
                    onClick = { activeSection = NavSection.SERIES; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.Radio,
                    label = "Radio",
                    isSelected = activeSection == NavSection.RADIO,
                    onClick = { activeSection = NavSection.RADIO; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.GridView,
                    label = "Hub",
                    isSelected = activeSection == NavSection.HUB,
                    onClick = { activeSection = NavSection.HUB; selectedFilter = "All" }
                )
            }

            // Right Quick Actions & Live Clock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Clock Pill
                Text(
                    text = if (currentTime.isNotBlank()) currentTime else "Merlin TV",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // Search Icon
                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (showSearchBar || searchQuery.isNotBlank()) PrimaryBlue else Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Update Notification Icon
                IconButton(
                    onClick = onOpenUpdateDialog,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (availableUpdate != null) LiveBadgeColor else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = if (availableUpdate != null) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Updates",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Favorites Heart Icon
                IconButton(
                    onClick = {
                        activeSection = NavSection.FAVORITES
                        selectedFilter = "All"
                        refreshFavorites()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (activeSection == NavSection.FAVORITES) AccentGold else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = if (activeSection == NavSection.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Settings Gear Icon
                IconButton(
                    onClick = onOpenSettingsDialog,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(visible = showSearchBar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TvSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "Search channels, movies, sports...",
                    modifier = Modifier.width(320.dp)
                )
            }
        }

        // =========================================================
        // 2. MAIN CONTENT: HOME / HUB HERO VIEW VS. CHANNEL GRID VIEW
        // =========================================================
        if (activeSection == NavSection.HOME || activeSection == NavSection.HUB) {
            // Scrollable Home / Hub Dashboard Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Feature Banner (CobraTV Pro Style Carousel)
                val heroSlides = listOf(
                    Triple("Sky & News Network", "The world, live in 1080p FHD", "Watch live Sky News UK, BBC News, Bloomberg Europe, France 24, DW & breaking global reporting.") to { activeSection = NavSection.SKY },
                    Triple("Pluto TV FAST Channels", "Binge TV & Unlimited Movies", "Explore hundreds of live FAST feeds across Crime, Drama, Comedy, Documentaries & Classic Hits.") to { activeSection = NavSection.PLUTO },
                    Triple("Cinema & Feature Films", "Top Movies & Classic Cinema", "Stream curated feature films, classic cinema, action, thriller & sci-fi channels.") to { activeSection = NavSection.MOVIES },
                    Triple("Worldwide Live TV", "1,500+ Live Broadcast Streams", "Instant live TV from the United Kingdom, USA, Canada, Australia, France, Germany & Europe.") to { activeSection = NavSection.LIVE }
                )

                val (slideInfo, slideAction) = heroSlides[heroSlideIndex]

                HeroFeatureBanner(
                    tag = "MerlinTV Hub",
                    title = slideInfo.second,
                    subtitle = slideInfo.third,
                    actionLabel = "Explore ${slideInfo.first}",
                    activeDotIndex = heroSlideIndex,
                    totalDots = 4,
                    onActionClick = slideAction
                )

                // Featured Hubs Row (CobraTV Pro 8-Card Showcase)
                Text(
                    text = "Featured Hubs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubShortcutCard(
                        title = "Live Cams & World",
                        subtitle = "Travel & city live streams",
                        icon = Icons.Default.Videocam,
                        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.LIVE; selectedFilter = "Travel" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "News Hub",
                        subtitle = "${skyChannels.size} live news feeds",
                        icon = Icons.AutoMirrored.Filled.Feed,
                        gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.SKY; selectedFilter = "All" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Sports Hub",
                        subtitle = "Highlights & live action",
                        icon = Icons.Default.SportsSoccer,
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.LIVE; selectedFilter = "Sports" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Game Shows & Ent.",
                        subtitle = "Comedy & entertainment",
                        icon = Icons.Default.EmojiEvents,
                        gradientColors = listOf(Color(0xFFD97706), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.SERIES; selectedFilter = "Entertainment" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Cinema Hub",
                        subtitle = "${movieChannels.size} feature films",
                        icon = Icons.Default.Movie,
                        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.MOVIES; selectedFilter = "All" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Series & Binge TV",
                        subtitle = "${seriesChannels.size} streaming series",
                        icon = Icons.Default.Tv,
                        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.SERIES; selectedFilter = "All" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Wildlife & Docs",
                        subtitle = "Nature & science docs",
                        icon = Icons.Default.Pets,
                        gradientColors = listOf(Color(0xFFEA580C), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.SERIES; selectedFilter = "Documentary" },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Radio & Music",
                        subtitle = "24/7 stations & hits",
                        icon = Icons.Default.Radio,
                        gradientColors = listOf(Color(0xFFE11D48), Color(0xFF0F172A)),
                        onClick = { activeSection = NavSection.RADIO; selectedFilter = "All" },
                        modifier = Modifier.width(220.dp)
                    )
                }

                // Recommended Channels Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recommended Channels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${currentItems.size} streams available",
                        color = AccentSky,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Recommended Grid in Home
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    itemsIndexed(
                        items = currentItems.take(15),
                        key = { index, it -> "${it.id}-${it.url}-$index" }
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
        } else {
            // =========================================================
            // 3. DEDICATED CHANNEL BROWSING VIEW (LIVE / PLUTO / SKY / MOVIES / SERIES)
            // =========================================================

            // Focused Channel Header Bar (Compact EPG Now & Next)
            FocusedChannelHeaderBar(
                item = focusedItem,
                channelNumber = focusedIndex,
                onWatchClick = {
                    focusedItem?.let { onSelectChannel(it, currentItems) }
                }
            )

            // Horizontal Filter Chips Row (Countries & Categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pills
                filterCategories.forEach { cat ->
                    val isSelected = selectedFilter.equals(cat, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                            .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(BorderSubtle))
                Spacer(modifier = Modifier.width(4.dp))

                // Country Pills
                countriesList.forEach { (code, label) ->
                    val isSelected = selectedFilter.equals(code, ignoreCase = true)
                    val count = countryCounts[code] ?: 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                            .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = code }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (count > 0) "$label ($count)" else label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 5-Column Channel Grid
            if (isLoading && currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentSky, modifier = Modifier.size(36.dp))
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
                        key = { index, it -> "${it.id}-${it.url}-$index" }
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