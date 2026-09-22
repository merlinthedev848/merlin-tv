package com.example.merlinmedia.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.ui.components.ChannelGridCard
import com.example.merlinmedia.ui.components.FocusedChannelHeaderBar
import com.example.merlinmedia.ui.theme.*

@Composable
fun LiveChannelSection(
    focusedItem: MediaEntry?,
    focusedIndex: Int,
    categories: List<String>,
    countries: List<Pair<String, String>>,
    countryCounts: Map<String, Int>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    items: List<MediaEntry>,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    activeSection: NavSection,
    onToggleFavorite: (String) -> Unit,
    onFocusChannel: (MediaEntry, Int) -> Unit,
    onSelectChannel: (MediaEntry, List<MediaEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Focused Channel Header Bar (EPG Now & Next)
        FocusedChannelHeaderBar(
            item = focusedItem,
            channelNumber = focusedIndex,
            onWatchClick = {
                focusedItem?.let { onSelectChannel(it, items) }
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
            categories.forEach { cat ->
                val isSelected = selectedFilter.equals(cat, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                        .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { onSelectFilter(cat) }
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

                countries.forEach { (code, label) ->
                    val isSelected = selectedFilter.equals(code, ignoreCase = true)
                    val count = countryCounts[code] ?: 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryBlue else Color(0xFF1E293B))
                            .border(1.dp, if (isSelected) AccentSky else BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { onSelectFilter(code) }
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
        if (isLoading && items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentSky, modifier = Modifier.size(36.dp))
            }
        } else if (items.isEmpty()) {
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
                    items = items,
                    key = { index, it -> "${it.id}-${it.url}-$index" }
                ) { index, item ->
                    val chNum = 101 + index
                    ChannelGridCard(
                        item = item,
                        channelNumber = chNum,
                        isFavorite = favoriteIds.contains(item.id),
                        onToggleFavorite = {
                            onToggleFavorite(item.id)
                        },
                        onFocusChange = {
                            onFocusChannel(item, chNum)
                        },
                        onClick = {
                            onSelectChannel(item, items)
                        }
                    )
                }
            }
        }
    }
}
