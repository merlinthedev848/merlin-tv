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
 * Modern Netflix-Style Streaming Dashboard for Merlin TV matching reference mockup.
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
    // 1. Curated Hero Billboard Item matching mockup (THE ALCHEMIST'S LEGACY)
    val defaultHeroItem = remember {
        MediaEntry(
            id = "hero-alchemist-legacy",
            title = "THE ALCHEMIST'S LEGACY",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            type = Kind.MOVIE,
            genre = "Action • Fantasy • Adventure",
            year = "2025",
            duration = "2h 15m",
            rating = "8.9",
            description = "A legendary wizard hunts a dangerous dark magic across centuries to protect a hidden world.",
            isVod = true,
            quality = "4K ULTRA HD",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=85"
        )
    }

    val featuredCandidates = remember(movieChannels, seriesChannels) {
        val list = mutableListOf<MediaEntry>()
        list.add(defaultHeroItem)
        if (movieChannels.isNotEmpty()) list.addAll(movieChannels.take(4))
        if (seriesChannels.isNotEmpty()) list.addAll(seriesChannels.take(2))
        list
    }

    var heroIndex by remember { mutableIntStateOf(0) }
    val currentHeroItem = featuredCandidates.getOrElse(heroIndex % featuredCandidates.size) { defaultHeroItem }

    // Auto-cycle hero billboard safely
    LaunchedEffect(featuredCandidates.size) {
        while (true) {
            kotlinx.coroutines.delay(12000)
            if (featuredCandidates.isNotEmpty()) {
                heroIndex = (heroIndex + 1) % featuredCandidates.size
            }
        }
    }

    // 2. Curated Top 10 Today Items matching mockup (Dungeon Kings, Starfall, The Witcher, Lost City, Ocean's End...)
    val curatedTop10 = remember {
        listOf(
            MediaEntry(
                id = "top10-dungeon-kings",
                title = "DUNGEON KINGS",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                type = Kind.MOVIE,
                genre = "Action, Fantasy",
                year = "2025",
                rating = "9.1 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-starfall",
                title = "STARFALL",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Adventure",
                year = "2025",
                rating = "8.8 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-the-witcher",
                title = "THE WITCHER",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                type = Kind.SERIES,
                genre = "Fantasy, Drama",
                year = "2024",
                rating = "8.9 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-lost-city",
                title = "LOST CITY",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                type = Kind.MOVIE,
                genre = "Adventure, Action",
                year = "2025",
                rating = "8.6 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-oceans-end",
                title = "OCEAN'S END",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                type = Kind.MOVIE,
                genre = "Documentary, Nature",
                year = "2024",
                rating = "9.0 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-cyberpunk-2099",
                title = "CYBERPUNK 2099",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Cyberpunk",
                year = "2025",
                rating = "8.7 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-valhalla",
                title = "VALHALLA",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                type = Kind.SERIES,
                genre = "Action, Norse",
                year = "2024",
                rating = "8.8 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-shadow-realm",
                title = "SHADOW REALM",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                type = Kind.MOVIE,
                genre = "Fantasy, Mystery",
                year = "2025",
                rating = "8.5 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-solaris",
                title = "SOLARIS",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Space",
                year = "2024",
                rating = "8.9 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80"
            ),
            MediaEntry(
                id = "top10-ironclad",
                title = "IRONCLAD",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                type = Kind.MOVIE,
                genre = "Action, Medieval",
                year = "2024",
                rating = "8.4 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80",
                logo = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80"
            )
        )
    }

    val top10Items = remember(curatedTop10, movieChannels, seriesChannels) {
        val list = mutableListOf<MediaEntry>()
        list.addAll(curatedTop10)
        if (movieChannels.isNotEmpty()) list.addAll(movieChannels.take(5))
        if (seriesChannels.isNotEmpty()) list.addAll(seriesChannels.take(3))
        list.distinctBy { it.id }.take(10)
    }

    // 3. Curated Trending Blockbuster Movies matching mockup (Matrix, Dune, Avengers, Blade Runner, Oppenheimer, Interstellar)
    val curatedBlockbusters = remember {
        listOf(
            MediaEntry(
                id = "blockbuster-matrix",
                title = "MATRIX",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Cyberpunk",
                year = "2024",
                rating = "9.2 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80"
            ),
            MediaEntry(
                id = "blockbuster-dune",
                title = "DUNE",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Epic",
                year = "2024",
                rating = "9.0 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80"
            ),
            MediaEntry(
                id = "blockbuster-avengers",
                title = "AVENGERS",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                type = Kind.MOVIE,
                genre = "Action, Superhero",
                year = "2024",
                rating = "8.8 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&q=80"
            ),
            MediaEntry(
                id = "blockbuster-blade-runner",
                title = "BLADE RUNNER",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Noir",
                year = "2024",
                rating = "8.9 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&q=80"
            ),
            MediaEntry(
                id = "blockbuster-oppenheimer",
                title = "OPPENHEIMER",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                type = Kind.MOVIE,
                genre = "Biography, Drama",
                year = "2024",
                rating = "9.1 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80"
            ),
            MediaEntry(
                id = "blockbuster-interstellar",
                title = "INTERSTELLAR",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                type = Kind.MOVIE,
                genre = "Sci-Fi, Space",
                year = "2024",
                rating = "9.3 ★",
                isVod = true,
                quality = "4K",
                backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
                logo = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&q=80"
            )
        )
    }

    val trendingBlockbusterList = remember(curatedBlockbusters, movieChannels) {
        val list = mutableListOf<MediaEntry>()
        list.addAll(curatedBlockbusters)
        list.addAll(movieChannels)
        list.distinctBy { it.id }
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

    // Full Root Layout with Left Navigation Rail and Main Streaming Area
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
    ) {
        // ==========================================
        // 1. LEFT SLIM DOCK (Vertical Icon Rail)
        // ==========================================
        NetflixLeftNavRail(
            activeSection = NavSection.HOME,
            onOpenSection = onOpenSection,
            onOpenSettings = onOpenSettings
        )

        // ==========================================
        // 2. MAIN CONTENT AREA
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 14.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ==========================================
            // TOP NAVIGATION BAR
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NetflixRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "M",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "MERLIN TV",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                    }

                    // Navigation Pills: [ Home | Movies | TV Series | Live TV | EPG Guide | My List ]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NetflixNavTab(label = "Home", isSelected = true, onClick = {})
                        NetflixNavTab(label = "Movies", isSelected = false, onClick = { onOpenSection(NavSection.MOVIES) })
                        NetflixNavTab(label = "TV Series", isSelected = false, onClick = { onOpenSection(NavSection.SERIES) })
                        NetflixNavTab(label = "Live TV", isSelected = false, onClick = { onOpenSection(NavSection.LIVE) })
                        NetflixNavTab(label = "EPG Guide", isSelected = false, onClick = { onOpenSection(NavSection.EPG) })
                        NetflixNavTab(label = "My List", isSelected = false, onClick = { onOpenSection(NavSection.FAVORITES) })
                    }
                }

                // Right: Utility Actions (Notification Bell with Badge, Profile Avatar, Search, Clock, Update, Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Action
                    NetflixIconButton(icon = Icons.Default.Search, label = "Search", onClick = onOpenSearch)

                    // Notification Bell with Red Dot Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x66181820))
                            .clickable { onOpenNotices() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        // Red Notification Badge Dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(NetflixRed)
                        )
                    }

                    // Profile Avatar with Dropdown Arrow
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x66181820))
                            .clickable { onOpenAccount() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "M",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Profile",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Live Clock Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF181820))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text("UPDATE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Settings Action
                    NetflixIconButton(icon = Icons.Default.Settings, label = "Settings", onClick = onOpenSettings)
                }
            }

            // ==========================================
            // SCROLLABLE STREAMING FEED
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Section A: Hero Spotlight Billboard
                item(key = "netflix_hero_billboard") {
                    NetflixHeroBillboard(
                        item = currentHeroItem,
                        isFavorite = favoriteIds.contains(currentHeroItem.id),
                        onToggleFavorite = { onToggleFavorite(currentHeroItem.id) },
                        onPlay = {
                            val playlist = if (currentHeroItem.type == Kind.SERIES) seriesChannels else movieChannels
                            onSelectChannel(currentHeroItem, playlist.ifEmpty { listOf(currentHeroItem) })
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

                // Section B: Top 10 Today (Giant Glowing Red Neon Numbers + Posters)
                if (top10Items.isNotEmpty()) {
                    item(key = "netflix_top_10_row") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Top 10 Today",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

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

                // Section C: Trending Blockbuster Movies (Matrix, Dune, Avengers, Blade Runner, Oppenheimer...)
                if (trendingBlockbusterList.isNotEmpty()) {
                    item(key = "netflix_trending_blockbusters") {
                        NetflixContentRow(
                            title = "Trending Blockbuster Movies",
                            icon = Icons.Default.Movie,
                            badgeText = "Explore All (${trendingBlockbusterList.size}) >",
                            items = trendingBlockbusterList.take(15),
                            favoriteIds = favoriteIds,
                            onToggleFavorite = onToggleFavorite,
                            onSelectItem = { onSelectMovieForDetails(it) }
                        )
                    }
                }

                // Section D: Continue Watching / History
                if (recentHistory.isNotEmpty()) {
                    item(key = "netflix_continue_watching") {
                        NetflixContentRow(
                            title = "Continue Watching",
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

                // Section E: Binge-Worthy TV Series
                if (seriesChannels.isNotEmpty()) {
                    item(key = "netflix_binge_series") {
                        NetflixContentRow(
                            title = "Binge-Worthy TV Series",
                            icon = Icons.Default.Tv,
                            badgeText = "Explore All (${seriesChannels.size}) >",
                            items = seriesChannels.take(15),
                            favoriteIds = favoriteIds,
                            onToggleFavorite = onToggleFavorite,
                            onSelectItem = { onSelectSeriesForEpisodes(it) }
                        )
                    }
                }

                // Section F: Action, Sci-Fi & Fantasy
                if (actionSciFiItems.isNotEmpty()) {
                    item(key = "netflix_action_scifi") {
                        NetflixContentRow(
                            title = "Action, Sci-Fi & Fantasy",
                            icon = Icons.Default.Bolt,
                            items = actionSciFiItems,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = onToggleFavorite,
                            onSelectItem = { onSelectMovieForDetails(it) }
                        )
                    }
                }

                // Section G: Live TV Channels & Top Broadcasts
                if (liveTopPicks.isNotEmpty()) {
                    item(key = "netflix_live_picks") {
                        NetflixContentRow(
                            title = "Live TV Channels & Top Broadcasts",
                            icon = Icons.Default.LiveTv,
                            badgeText = "All Channels >",
                            items = liveTopPicks.take(15),
                            favoriteIds = favoriteIds,
                            onToggleFavorite = onToggleFavorite,
                            onSelectItem = { onSelectChannel(it, liveTopPicks) }
                        )
                    }
                }

                // Section H: Classic Cinema & Timeless Favourites
                if (classicDramaItems.isNotEmpty()) {
                    item(key = "netflix_classic_drama") {
                        NetflixContentRow(
                            title = "Classic Cinema & Timeless Favourites",
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
                            title = "My Watchlist",
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
            // BOTTOM PILL QUICK UTILITIES
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
}


