package com.example.merlinmedia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.data.EpgRepository
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.components.*
import com.example.merlinmedia.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

enum class SortMode(val label: String) {
    DEFAULT("Default"),
    ALPHABETICAL("A to Z"),
    QUALITY("4K / FHD First"),
    COUNTRY("By Country")
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeSection by remember { mutableStateOf(NavSection.HOME) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var currentSortMode by remember { mutableStateOf(SortMode.DEFAULT) }

    val favoriteIds by favoritesManager.favoriteIds.collectAsState()
    val recentHistory by favoritesManager.recentHistory.collectAsState()

    var focusedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var focusedIndex by remember { mutableIntStateOf(101) }

    // VOD Dialog State
    var selectedMovieForDetails by remember { mutableStateOf<MediaEntry?>(null) }
    var selectedSeriesForEpisodes by remember { mutableStateOf<MediaEntry?>(null) }

    // Hero Carousel Index
    var heroSlideIndex by remember { mutableIntStateOf(0) }

    // Debounce search query to prevent stutter on 5,000+ item grids
    LaunchedEffect(searchQuery) {
        delay(200L)
        debouncedSearchQuery = searchQuery
    }

    // Live Clock State
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = java.text.SimpleDateFormat("h:mm a  EEE, dd MMM", java.util.Locale.getDefault())
        while (true) {
            currentTime = formatter.format(java.util.Date())
            delay(1000)
        }
    }

    // Initialize EPG repository in background
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            EpgRepository.initialize(context)
        }
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

    val countryCounts = remember(liveChannels) {
        countriesList.associate { (code, _) ->
            val count = liveChannels.count { item ->
                item.country.equals(code, ignoreCase = true) ||
                item.group.startsWith("$code |", ignoreCase = true)
            }
            code to count
        }
    }

    val liveFilterCategories = listOf(
        "All", "News", "Sports", "Entertainment", "Series & Drama",
        "Documentaries", "Kids", "Animation", "Comedy", "Music", "Cooking",
        "Travel", "Science", "Education", "Business", "Weather", "Classic", "Auto"
    )

    val movieFilterGenres = listOf(
        "All", "Sci-Fi", "Action", "Horror", "Thriller", "Comedy", "Drama", "Fantasy", "Classic", "Animation"
    )

    val seriesFilterGenres = listOf(
        "All", "Comedy", "Western", "Animation", "Drama", "Sci-Fi", "Classic"
    )

    val heroSlides = listOf(
        Triple("VOD Movies & Releases", "Feature Films On Demand", "Stream full-length high quality movies, classic cinema & cult favourites with synopsis and ratings.") to { activeSection = NavSection.MOVIES },
        Triple("Episodic TV Series", "Binge-Worthy Series On Demand", "Watch complete seasons and episodes of all-time classic TV series with episode guide.") to { activeSection = NavSection.SERIES },
        Triple("Sky & News Network", "The world, live in 1080p FHD", "Watch live Sky News UK, BBC News, Bloomberg Europe, France 24, DW & breaking global reporting.") to { activeSection = NavSection.SKY },
        Triple("Worldwide Live TV", "1,500+ Live Broadcast Streams", "Instant live TV from the United Kingdom, USA, Canada, Australia, France, Germany & Europe.") to { activeSection = NavSection.LIVE }
    )

    // Auto-cycle Hero Carousel safely
    LaunchedEffect(heroSlides.size) {
        while (true) {
            delay(8000)
            if (heroSlides.isNotEmpty()) {
                heroSlideIndex = (heroSlideIndex + 1) % heroSlides.size
            }
        }
    }

    // High-performance filter & sort derivation
    val currentItems = remember(activeSection, selectedFilter, debouncedSearchQuery, currentSortMode, liveChannels, plutoChannels, skyChannels, movieChannels, seriesChannels, favoriteIds) {
        val baseList = when (activeSection) {
            NavSection.HOME, NavSection.HUB -> skyChannels + plutoChannels.take(40) + liveChannels.take(40) + movieChannels.take(20)
            NavSection.LIVE -> liveChannels
            NavSection.PLUTO -> plutoChannels
            NavSection.SKY -> skyChannels
            NavSection.MOVIES -> movieChannels
            NavSection.SERIES -> seriesChannels
            NavSection.RADIO -> liveChannels.filter { it.group.contains("music", ignoreCase = true) || it.title.contains("radio", ignoreCase = true) }
            NavSection.FAVORITES -> {
                val allKnown = liveChannels + plutoChannels + skyChannels + movieChannels + seriesChannels
                allKnown.filter { favoriteIds.contains(it.id) }
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
                    item.genre.contains(selectedFilter, ignoreCase = true) ||
                    item.title.contains(selectedFilter, ignoreCase = true)
                }
            }
        }

        val searchedList = if (debouncedSearchQuery.isBlank()) {
            filteredByFilter
        } else {
            val q = debouncedSearchQuery.trim().lowercase()
            filteredByFilter.filter { item ->
                item.title.lowercase().contains(q) ||
                item.group.lowercase().contains(q) ||
                item.genre.lowercase().contains(q) ||
                item.country.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            }
        }

        // Apply Sorting
        when (currentSortMode) {
            SortMode.DEFAULT -> searchedList
            SortMode.ALPHABETICAL -> searchedList.sortedBy { it.title.lowercase() }
            SortMode.QUALITY -> searchedList.sortedByDescending {
                when {
                    it.quality.contains("4K", ignoreCase = true) -> 4
                    it.quality.contains("1080", ignoreCase = true) -> 3
                    it.quality.contains("720", ignoreCase = true) -> 2
                    else -> 1
                }
            }
            SortMode.COUNTRY -> searchedList.sortedBy { it.country.lowercase() }
        }
    }

    // Keep focusedItem in sync
    LaunchedEffect(currentItems) {
        if (currentItems.isNotEmpty() && (focusedItem == null || !currentItems.contains(focusedItem))) {
            focusedItem = currentItems.first()
            focusedIndex = 101
        } else if (focusedItem != null) {
            val idx = currentItems.indexOf(focusedItem)
            if (idx >= 0) focusedIndex = 101 + idx
        }
    }

    // Render VOD Movie Details Dialog when selected
    selectedMovieForDetails?.let { movie ->
        VodDetailsDialog(
            item = movie,
            isFavorite = favoriteIds.contains(movie.id),
            onToggleFavorite = {
                favoritesManager.toggleFavorite(movie.id)
            },
            onPlay = {
                onSelectChannel(movie, movieChannels)
                selectedMovieForDetails = null
            },
            onDismiss = { selectedMovieForDetails = null }
        )
    }

    // Render VOD Series Episode Dialog when selected
    selectedSeriesForEpisodes?.let { series ->
        val baseTitle = series.title.substringBefore(":").trim()
        val allEpisodesForSeries = seriesChannels.filter {
            it.title.startsWith(baseTitle, ignoreCase = true) || it.id.startsWith(series.id.substringBefore("-s1"))
        }
        SeriesEpisodeDialog(
            item = series,
            allEpisodes = if (allEpisodesForSeries.isNotEmpty()) allEpisodesForSeries else listOf(series),
            onSelectEpisode = { ep ->
                onSelectChannel(ep, seriesChannels)
                selectedSeriesForEpisodes = null
            },
            onDismiss = { selectedSeriesForEpisodes = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Offline Warning Banner
        if (!isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFB91C1C))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "You are currently offline. Showing cached channels and downloads.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onRefreshChannels,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Retry", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // 1. TOP HORIZONTAL NAVIGATION & ACTION BAR
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding
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

            // Horizontal Navigation Menu Tabs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .clipToBounds()
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
                    icon = Icons.Default.Sensors,
                    label = "Sky & News",
                    isSelected = activeSection == NavSection.SKY,
                    onClick = { activeSection = NavSection.SKY; selectedFilter = "All" }
                )
                TopNavBarItem(
                    icon = Icons.Default.Language,
                    label = "Pluto TV",
                    isSelected = activeSection == NavSection.PLUTO,
                    onClick = { activeSection = NavSection.PLUTO; selectedFilter = "All" }
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
                Text(
                    text = if (currentTime.isNotBlank()) currentTime else "Merlin TV",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // Sort Mode Button
                IconButton(
                    onClick = {
                        currentSortMode = when (currentSortMode) {
                            SortMode.DEFAULT -> SortMode.ALPHABETICAL
                            SortMode.ALPHABETICAL -> SortMode.QUALITY
                            SortMode.QUALITY -> SortMode.COUNTRY
                            SortMode.COUNTRY -> SortMode.DEFAULT
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (currentSortMode != SortMode.DEFAULT) AccentSky.copy(alpha = 0.3f) else Color(0xFF1E293B))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort: ${currentSortMode.label}", tint = Color.White, modifier = Modifier.size(16.dp))
                }

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

        // Expandable Search Bar & Active Sort Indicator
        AnimatedVisibility(visible = showSearchBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSortMode != SortMode.DEFAULT) {
                    Text(
                        text = "Sorting: ${currentSortMode.label}",
                        color = AccentSky,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                TvSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "Search channels, movies, sports...",
                    modifier = Modifier.width(320.dp)
                )
            }
        }

        // =========================================================
        // 2. MAIN CONTENT VIEW (ISOLATED COLUMN LAYOUTS)
        // =========================================================
        if (activeSection == NavSection.HOME || activeSection == NavSection.HUB) {
            // Self-contained LazyColumn where each section is an isolated block
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section 1: Hero Feature Banner
                if (heroSlides.isNotEmpty()) {
                    item(key = "home_hero_banner") {
                        val safeIndex = heroSlideIndex.coerceIn(0, heroSlides.size - 1)
                        val (slideInfo, slideAction) = heroSlides[safeIndex]

                        HeroFeatureBanner(
                            tag = "MerlinTV Feature",
                            title = slideInfo.second,
                            subtitle = slideInfo.third,
                            actionLabel = "Explore ${slideInfo.first}",
                            activeDotIndex = safeIndex,
                            totalDots = heroSlides.size,
                            onActionClick = slideAction,
                            modifier = Modifier.fillMaxWidth().clipToBounds()
                        )
                    }
                }

                // Section 2: Recently Watched Row
                if (recentHistory.isNotEmpty()) {
                    item(key = "home_recently_watched") {
                        Column(
                            modifier = Modifier.fillMaxWidth().clipToBounds(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = AccentSky, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Recently Watched / History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().clipToBounds()
                            ) {
                                items(recentHistory.take(12), key = { "recent-${it.id}-${it.url}" }) { item ->
                                    if (item.isVod) {
                                        VodMovieCard(
                                            item = item,
                                            isFavorite = favoriteIds.contains(item.id),
                                            onToggleFavorite = { favoritesManager.toggleFavorite(item.id) },
                                            onClick = {
                                                if (item.type == Kind.SERIES) selectedSeriesForEpisodes = item
                                                else selectedMovieForDetails = item
                                            }
                                        )
                                    } else {
                                        ChannelGridCard(
                                            item = item,
                                            channelNumber = 101,
                                            isFavorite = favoriteIds.contains(item.id),
                                            onToggleFavorite = { favoritesManager.toggleFavorite(item.id) },
                                            onFocusChange = { focusedItem = item },
                                            onClick = { onSelectChannel(item, recentHistory) },
                                            modifier = Modifier.width(190.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Featured Hubs Row
                item(key = "home_featured_hubs") {
                    Column(
                        modifier = Modifier.fillMaxWidth().clipToBounds(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Featured Hubs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .clipToBounds(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HubShortcutCard(
                                title = "Movies (VOD)",
                                subtitle = "${movieChannels.size} feature films",
                                icon = Icons.Default.PlayCircle,
                                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.MOVIES; selectedFilter = "All" },
                                modifier = Modifier.width(220.dp)
                            )

                            HubShortcutCard(
                                title = "Series (VOD)",
                                subtitle = "${seriesChannels.size} TV series episodes",
                                icon = Icons.Default.Movie,
                                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.SERIES; selectedFilter = "All" },
                                modifier = Modifier.width(220.dp)
                            )

                            HubShortcutCard(
                                title = "Live Broadcast TV",
                                subtitle = "${liveChannels.size} live streams",
                                icon = Icons.Default.LiveTv,
                                gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.LIVE; selectedFilter = "All" },
                                modifier = Modifier.width(220.dp)
                            )

                            HubShortcutCard(
                                title = "Sky & News Hub",
                                subtitle = "${skyChannels.size} live news feeds",
                                icon = Icons.AutoMirrored.Filled.Feed,
                                gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.SKY; selectedFilter = "All" },
                                modifier = Modifier.width(220.dp)
                            )

                            HubShortcutCard(
                                title = "Pluto TV FAST",
                                subtitle = "${plutoChannels.size} 24/7 channels",
                                icon = Icons.Default.Language,
                                gradientColors = listOf(Color(0xFFD97706), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.PLUTO; selectedFilter = "All" },
                                modifier = Modifier.width(220.dp)
                            )

                            HubShortcutCard(
                                title = "Sports Hub",
                                subtitle = "Highlights & live sports",
                                icon = Icons.Default.SportsSoccer,
                                gradientColors = listOf(Color(0xFF059669), Color(0xFF0F172A)),
                                onClick = { activeSection = NavSection.LIVE; selectedFilter = "Sports" },
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
                    }
                }

                // Section 4: Featured Movies Row
                if (movieChannels.isNotEmpty()) {
                    item(key = "home_featured_movies") {
                        Column(
                            modifier = Modifier.fillMaxWidth().clipToBounds(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = AccentSky, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Featured Movies (On Demand)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "View All Movies >",
                                    color = AccentSky,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { activeSection = NavSection.MOVIES; selectedFilter = "All" }
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth().clipToBounds()
                            ) {
                                items(movieChannels.take(10), key = { "home-movie-${it.id}" }) { movie ->
                                    VodMovieCard(
                                        item = movie,
                                        isFavorite = favoriteIds.contains(movie.id),
                                        onToggleFavorite = {
                                            favoritesManager.toggleFavorite(movie.id)
                                        },
                                        onClick = { selectedMovieForDetails = movie }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 5: Featured Series Row
                if (seriesChannels.isNotEmpty()) {
                    item(key = "home_featured_series") {
                        Column(
                            modifier = Modifier.fillMaxWidth().clipToBounds(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Featured Series (On Demand)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "View All Series >",
                                    color = AccentSky,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { activeSection = NavSection.SERIES; selectedFilter = "All" }
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth().clipToBounds()
                            ) {
                                items(seriesChannels.take(10), key = { "home-series-${it.id}" }) { series ->
                                    VodMovieCard(
                                        item = series,
                                        isFavorite = favoriteIds.contains(series.id),
                                        onToggleFavorite = {
                                            favoritesManager.toggleFavorite(series.id)
                                        },
                                        onClick = { selectedSeriesForEpisodes = series }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 6: Live TV Highlights Grid
                item(key = "home_live_highlights_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live TV Highlights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${liveChannels.size + plutoChannels.size + skyChannels.size} live channels",
                            color = AccentSky,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Chunked 5-item rows for perfectly isolated channel grid rows in Home LazyColumn
                val highlightItems = currentItems.take(20)
                val chunkedRows = highlightItems.chunked(5)
                itemsIndexed(chunkedRows, key = { rowIndex, _ -> "home_grid_row_$rowIndex" }) { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clipToBounds(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEachIndexed { colIndex, item ->
                            val chNum = 101 + (rowIndex * 5) + colIndex
                            ChannelGridCard(
                                item = item,
                                channelNumber = chNum,
                                isFavorite = favoriteIds.contains(item.id),
                                onToggleFavorite = { favoritesManager.toggleFavorite(item.id) },
                                onFocusChange = { focusedItem = item; focusedIndex = chNum },
                                onClick = { onSelectChannel(item, currentItems) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining spaces in partial row to maintain width alignment
                        if (rowItems.size < 5) {
                            for (i in 0 until (5 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else if (activeSection == NavSection.MOVIES) {
            // Dedicated, bounded Movies Section Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎬 Movies (VOD On Demand)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Real on-demand feature films with synopses, ratings, and instant playback.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.2f))
                            .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentItems.size} Films Available",
                            color = AccentSky,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Movie Genre Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    movieFilterGenres.forEach { genre ->
                        val isSelected = selectedFilter.equals(genre, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { selectedFilter = genre }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // 6-Column 2:3 Movie Poster Grid
                if (currentItems.isEmpty()) {
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
                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Text("No movies found for '$selectedFilter'", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Try selecting 'All' or searching for another title.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clipToBounds(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = currentItems,
                            key = { index, it -> "vod-movie-${it.id}-$index" }
                        ) { _, movie ->
                            VodMovieCard(
                                item = movie,
                                isFavorite = favoriteIds.contains(movie.id),
                                onToggleFavorite = {
                                    favoritesManager.toggleFavorite(movie.id)
                                },
                                onClick = {
                                    selectedMovieForDetails = movie
                                }
                            )
                        }
                    }
                }
            }
        } else if (activeSection == NavSection.SERIES) {
            // Dedicated, bounded Series Section Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Series Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📺 TV Series (VOD On Demand)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Episodic television series on demand with full seasons and episode picker.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4F46E5).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF4F46E5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentItems.size} Series Available",
                            color = AccentSky,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Series Genre Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    seriesFilterGenres.forEach { genre ->
                        val isSelected = selectedFilter.equals(genre, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { selectedFilter = genre }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // 6-Column 2:3 Series Poster Grid
                if (currentItems.isEmpty()) {
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
                            Icon(Icons.Default.Tv, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Text("No series found for '$selectedFilter'", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Try selecting 'All' or searching for another title.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clipToBounds(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = currentItems,
                            key = { index, it -> "vod-series-${it.id}-$index" }
                        ) { _, series ->
                            VodMovieCard(
                                item = series,
                                isFavorite = favoriteIds.contains(series.id),
                                onToggleFavorite = {
                                    favoritesManager.toggleFavorite(series.id)
                                },
                                onClick = {
                                    selectedSeriesForEpisodes = series
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Dedicated Live / Pluto / Sky / Radio / Favorites Section Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focused Channel Header Bar (EPG Now & Next)
                FocusedChannelHeaderBar(
                    item = focusedItem,
                    channelNumber = focusedIndex,
                    onWatchClick = {
                        focusedItem?.let { onSelectChannel(it, currentItems) }
                    },
                    modifier = Modifier.fillMaxWidth().clipToBounds()
                )

                // Horizontal Filter Chips Row (Categories & Countries)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clipToBounds(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    liveFilterCategories.forEach { cat ->
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

                    if (activeSection == NavSection.LIVE || activeSection == NavSection.FAVORITES) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(BorderSubtle))
                        Spacer(modifier = Modifier.width(4.dp))

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
                            .clipToBounds(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = currentItems,
                            key = { index, it -> "${it.id}-${it.url}-$index" }
                        ) { index, item ->
                            val chNum = 101 + index
                            ChannelGridCard(
                                item = item,
                                channelNumber = chNum,
                                isFavorite = favoriteIds.contains(item.id),
                                onToggleFavorite = {
                                    favoritesManager.toggleFavorite(item.id)
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
}