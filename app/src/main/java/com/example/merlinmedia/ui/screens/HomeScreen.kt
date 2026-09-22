package com.example.merlinmedia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
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
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.model.SortMode
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.components.SeriesEpisodeDialog
import com.example.merlinmedia.ui.components.TvSearchBar
import com.example.merlinmedia.ui.components.VodDetailsDialog
import com.example.merlinmedia.ui.components.home.HomeHeroAndHubsSection
import com.example.merlinmedia.ui.components.home.HomeTopBar
import com.example.merlinmedia.ui.components.home.LiveChannelSection
import com.example.merlinmedia.ui.components.home.VodCatalogSection
import com.example.merlinmedia.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val countriesList = remember {
        listOf(
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
    }

    val countryCounts = remember(liveChannels) {
        countriesList.associate { (code, _) ->
            val count = liveChannels.count { item ->
                item.country.equals(code, ignoreCase = true) ||
                item.group.startsWith("$code |", ignoreCase = true)
            }
            code to count
        }
    }

    val liveFilterCategories = remember {
        listOf(
            "All", "News", "Sports", "Entertainment", "Series & Drama",
            "Documentaries", "Kids", "Animation", "Comedy", "Music", "Cooking",
            "Travel", "Science", "Education", "Business", "Weather", "Classic", "Auto"
        )
    }

    val movieFilterGenres = remember {
        listOf(
            "All", "Sci-Fi", "Action", "Horror", "Thriller", "Comedy", "Drama", "Fantasy", "Classic", "Animation"
        )
    }

    val seriesFilterGenres = remember {
        listOf(
            "All", "Comedy", "Western", "Animation", "Drama", "Sci-Fi", "Classic"
        )
    }

    val heroSlides = remember {
        listOf(
            Triple("VOD Movies & Releases", "Feature Films On Demand", "Stream full-length high quality movies, classic cinema & cult favourites with synopsis and ratings.") to { activeSection = NavSection.MOVIES; selectedFilter = "All" },
            Triple("Episodic TV Series", "Binge-Worthy Series On Demand", "Watch complete seasons and episodes of all-time classic TV series with episode guide.") to { activeSection = NavSection.SERIES; selectedFilter = "All" },
            Triple("Sky & News Network", "The world, live in 1080p FHD", "Watch live Sky News UK, BBC News, Bloomberg Europe, France 24, DW & breaking global reporting.") to { activeSection = NavSection.SKY; selectedFilter = "All" },
            Triple("Worldwide Live TV", "1,500+ Live Broadcast Streams", "Instant live TV from the United Kingdom, USA, Canada, Australia, France, Germany & Europe.") to { activeSection = NavSection.LIVE; selectedFilter = "All" }
        )
    }

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

        // Top Navigation Bar
        HomeTopBar(
            activeSection = activeSection,
            onSectionChange = {
                activeSection = it
                selectedFilter = "All"
            },
            currentTime = currentTime,
            currentSortMode = currentSortMode,
            onCycleSortMode = {
                currentSortMode = when (currentSortMode) {
                    SortMode.DEFAULT -> SortMode.ALPHABETICAL
                    SortMode.ALPHABETICAL -> SortMode.QUALITY
                    SortMode.QUALITY -> SortMode.COUNTRY
                    SortMode.COUNTRY -> SortMode.DEFAULT
                }
            },
            showSearchBar = showSearchBar,
            hasSearchQuery = searchQuery.isNotBlank(),
            onToggleSearchBar = { showSearchBar = !showSearchBar },
            availableUpdate = availableUpdate,
            onOpenUpdateDialog = onOpenUpdateDialog,
            onOpenSettingsDialog = onOpenSettingsDialog
        )

        // Expandable Search Bar
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

        // Main Content Area
        when (activeSection) {
            NavSection.HOME, NavSection.HUB -> {
                HomeHeroAndHubsSection(
                    heroSlides = heroSlides,
                    heroSlideIndex = heroSlideIndex,
                    recentHistory = recentHistory,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { favoritesManager.toggleFavorite(it) },
                    movieChannels = movieChannels,
                    seriesChannels = seriesChannels,
                    liveChannels = liveChannels,
                    plutoChannels = plutoChannels,
                    skyChannels = skyChannels,
                    currentItems = currentItems,
                    onSelectSection = {
                        activeSection = it
                        selectedFilter = "All"
                    },
                    onSelectMovieForDetails = { selectedMovieForDetails = it },
                    onSelectSeriesForEpisodes = { selectedSeriesForEpisodes = it },
                    onSelectChannel = onSelectChannel,
                    onFocusChannel = { item, idx ->
                        focusedItem = item
                        focusedIndex = idx
                    }
                )
            }
            NavSection.MOVIES -> {
                VodCatalogSection(
                    title = "🎬 Movies (VOD On Demand)",
                    subtitle = "Real on-demand feature films with synopses, ratings, and instant playback.",
                    badgeText = "${currentItems.size} Films Available",
                    badgeColor = PrimaryBlue,
                    genres = movieFilterGenres,
                    selectedFilter = selectedFilter,
                    onSelectGenre = { selectedFilter = it },
                    items = currentItems,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { favoritesManager.toggleFavorite(it) },
                    onSelectItem = { selectedMovieForDetails = it },
                    isSeries = false
                )
            }
            NavSection.SERIES -> {
                VodCatalogSection(
                    title = "📺 TV Series (VOD On Demand)",
                    subtitle = "Episodic television series on demand with full seasons and episode picker.",
                    badgeText = "${currentItems.size} Series Available",
                    badgeColor = Color(0xFF4F46E5),
                    genres = seriesFilterGenres,
                    selectedFilter = selectedFilter,
                    onSelectGenre = { selectedFilter = it },
                    items = currentItems,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { favoritesManager.toggleFavorite(it) },
                    onSelectItem = { selectedSeriesForEpisodes = it },
                    isSeries = true
                )
            }
            else -> {
                LiveChannelSection(
                    focusedItem = focusedItem,
                    focusedIndex = focusedIndex,
                    categories = liveFilterCategories,
                    countries = countriesList,
                    countryCounts = countryCounts,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    items = currentItems,
                    isLoading = isLoading,
                    favoriteIds = favoriteIds,
                    activeSection = activeSection,
                    onToggleFavorite = { favoritesManager.toggleFavorite(it) },
                    onFocusChannel = { item, idx ->
                        focusedItem = item
                        focusedIndex = idx
                    },
                    onSelectChannel = onSelectChannel
                )
            }
        }
    }
}