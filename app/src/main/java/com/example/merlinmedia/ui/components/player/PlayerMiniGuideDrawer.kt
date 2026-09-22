package com.example.merlinmedia.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.ui.components.QuickChannelDrawerItem
import com.example.merlinmedia.ui.components.TvSearchBar
import com.example.merlinmedia.ui.theme.*

@Composable
fun PlayerMiniGuideDrawer(
    playlist: List<MediaEntry>,
    currentItem: MediaEntry,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit,
    onSelectItem: (MediaEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, playlist) {
        if (searchQuery.isBlank()) playlist
        else {
            val q = searchQuery.trim().lowercase()
            playlist.filter {
                it.title.lowercase().contains(q) || it.group.lowercase().contains(q) || it.country.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(380.dp)
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorder)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Channel Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            TvSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholderText = "Filter ${playlist.size} channels..."
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filteredList, key = { index, it -> "${it.id}-${it.url}-$index" }) { index, item ->
                    QuickChannelDrawerItem(
                        item = item,
                        channelNumber = 101 + index,
                        isCurrentPlaying = item.id == currentItem.id || item.url == currentItem.url,
                        isFavorite = isFavorite(item.id),
                        onToggleFavorite = {
                            onToggleFavorite(item.id)
                        },
                        onClick = {
                            onSelectItem(item)
                            onClose()
                        }
                    )
                }
            }
        }
    }
}
