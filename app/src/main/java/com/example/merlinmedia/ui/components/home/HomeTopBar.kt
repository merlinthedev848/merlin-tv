package com.example.merlinmedia.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.model.SortMode
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.components.TopNavBarItem
import com.example.merlinmedia.ui.theme.*

@Composable
fun HomeTopBar(
    activeSection: NavSection,
    onSectionChange: (NavSection) -> Unit,
    currentTime: String,
    currentSortMode: SortMode,
    onCycleSortMode: () -> Unit,
    showSearchBar: Boolean,
    hasSearchQuery: Boolean,
    onToggleSearchBar: () -> Unit,
    availableUpdate: UpdateInfo?,
    onOpenUpdateDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── App Branding ──────────────────────────────────────────────
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

        // ── Horizontal Navigation Tabs (logical media order) ──────────
        // Order: Home → Live → Sky & News → Pluto TV → Movies → Series → Radio → Favorites
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
                onClick = { onSectionChange(NavSection.HOME) }
            )
            TopNavBarItem(
                icon = Icons.Default.LiveTv,
                label = "Live",
                isSelected = activeSection == NavSection.LIVE,
                onClick = { onSectionChange(NavSection.LIVE) }
            )
            TopNavBarItem(
                icon = Icons.Default.Sensors,
                label = "Sky & News",
                isSelected = activeSection == NavSection.SKY,
                onClick = { onSectionChange(NavSection.SKY) }
            )
            TopNavBarItem(
                icon = Icons.Default.Language,
                label = "Pluto TV",
                isSelected = activeSection == NavSection.PLUTO,
                onClick = { onSectionChange(NavSection.PLUTO) }
            )
            TopNavBarItem(
                icon = Icons.Default.PlayCircle,
                label = "Movies",
                isSelected = activeSection == NavSection.MOVIES,
                onClick = { onSectionChange(NavSection.MOVIES) }
            )
            TopNavBarItem(
                icon = Icons.Default.Movie,
                label = "Series",
                isSelected = activeSection == NavSection.SERIES,
                onClick = { onSectionChange(NavSection.SERIES) }
            )
            TopNavBarItem(
                icon = Icons.Default.Radio,
                label = "Radio",
                isSelected = activeSection == NavSection.RADIO,
                onClick = { onSectionChange(NavSection.RADIO) }
            )
            // Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF334155))
            )
            // Favorites moved into nav tabs where it belongs logically
            TopNavBarItem(
                icon = if (activeSection == NavSection.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "Favourites",
                isSelected = activeSection == NavSection.FAVORITES,
                onClick = { onSectionChange(NavSection.FAVORITES) }
            )
        }

        // ── Right Quick Actions & Live Clock ──────────────────────────
        // Clean minimal set: clock · sort · search · update · settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live Clock
            if (currentTime.isNotBlank()) {
                Text(
                    text = currentTime,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }

            // Sort Mode
            IconButton(
                onClick = onCycleSortMode,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentSortMode != SortMode.DEFAULT) AccentSky.copy(alpha = 0.3f)
                        else Color(0xFF1E293B)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort: ${currentSortMode.label}",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Search
            IconButton(
                onClick = onToggleSearchBar,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (showSearchBar || hasSearchQuery) PrimaryBlue else Color(0xFF1E293B)
                    )
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Update — prominent button when available, quiet icon when not
            if (availableUpdate != null) {
                Button(
                    onClick = onOpenUpdateDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = LiveBadgeColor),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "UPDATE v${availableUpdate.version}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenUpdateDialog,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Check for Updates",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Settings
            IconButton(
                onClick = onOpenSettingsDialog,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
