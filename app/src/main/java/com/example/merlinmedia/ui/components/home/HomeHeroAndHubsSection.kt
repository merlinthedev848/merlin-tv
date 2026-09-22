package com.example.merlinmedia.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.ui.components.*
import com.example.merlinmedia.ui.theme.*

@Composable
fun HomeHeroAndHubsSection(
    heroSlides: List<Pair<Triple<String, String, String>, () -> Unit>>,
    heroSlideIndex: Int,
    recentHistory: List<MediaEntry>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    movieChannels: List<MediaEntry>,
    seriesChannels: List<MediaEntry>,
    liveChannels: List<MediaEntry>,
    plutoChannels: List<MediaEntry>,
    skyChannels: List<MediaEntry>,
    currentItems: List<MediaEntry>,
    onSelectSection: (NavSection) -> Unit,
    onSelectMovieForDetails: (MediaEntry) -> Unit,
    onSelectSeriesForEpisodes: (MediaEntry) -> Unit,
    onSelectChannel: (MediaEntry, List<MediaEntry>) -> Unit,
    onFocusChannel: (MediaEntry, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
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
                                    onToggleFavorite = { onToggleFavorite(item.id) },
                                    onClick = {
                                        if (item.type == Kind.SERIES) onSelectSeriesForEpisodes(item)
                                        else onSelectMovieForDetails(item)
                                    }
                                )
                            } else {
                                ChannelGridCard(
                                    item = item,
                                    channelNumber = 101,
                                    isFavorite = favoriteIds.contains(item.id),
                                    onToggleFavorite = { onToggleFavorite(item.id) },
                                    onFocusChange = { onFocusChannel(item, 101) },
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
                        onClick = { onSelectSection(NavSection.MOVIES) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Series (VOD)",
                        subtitle = "${seriesChannels.size} TV series episodes",
                        icon = Icons.Default.Movie,
                        gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.SERIES) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Live Broadcast TV",
                        subtitle = "${liveChannels.size} live streams",
                        icon = Icons.Default.LiveTv,
                        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.LIVE) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Sky & News Hub",
                        subtitle = "${skyChannels.size} live news feeds",
                        icon = Icons.AutoMirrored.Filled.Feed,
                        gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.SKY) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Pluto TV FAST",
                        subtitle = "${plutoChannels.size} 24/7 channels",
                        icon = Icons.Default.Language,
                        gradientColors = listOf(Color(0xFFD97706), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.PLUTO) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Sports Hub",
                        subtitle = "Highlights & live sports",
                        icon = Icons.Default.SportsSoccer,
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.LIVE) },
                        modifier = Modifier.width(220.dp)
                    )

                    HubShortcutCard(
                        title = "Radio & Music",
                        subtitle = "24/7 stations & hits",
                        icon = Icons.Default.Radio,
                        gradientColors = listOf(Color(0xFFE11D48), Color(0xFF0F172A)),
                        onClick = { onSelectSection(NavSection.RADIO) },
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
                            modifier = Modifier.clickable { onSelectSection(NavSection.MOVIES) }
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
                                onToggleFavorite = { onToggleFavorite(movie.id) },
                                onClick = { onSelectMovieForDetails(movie) }
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
                            modifier = Modifier.clickable { onSelectSection(NavSection.SERIES) }
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
                                onToggleFavorite = { onToggleFavorite(series.id) },
                                onClick = { onSelectSeriesForEpisodes(series) }
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
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onFocusChange = { onFocusChannel(item, chNum) },
                        onClick = { onSelectChannel(item, currentItems) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size < 5) {
                    for (i in 0 until (5 - rowItems.size)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
