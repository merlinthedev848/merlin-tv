package com.example.merlinmedia.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.example.merlinmedia.ui.theme.*

data class TrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

@Composable
fun AudioAndSubtitleDialog(
    player: ExoPlayer,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Audio, 1: Subtitles

    val currentTracks = player.currentTracks

    val audioTracks = remember(currentTracks) {
        val list = mutableListOf<TrackItem>()
        var itemNum = 1
        for (g in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[g]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (t in 0 until group.length) {
                    val format = group.getTrackFormat(t)
                    val lang = format.language?.uppercase() ?: "UND"
                    val label = format.label ?: "Audio Track $itemNum ($lang)"
                    list.add(
                        TrackItem(
                            groupIndex = g,
                            trackIndex = t,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(t)
                        )
                    )
                    itemNum++
                }
            }
        }
        list
    }

    val subtitleTracks = remember(currentTracks) {
        val list = mutableListOf<TrackItem>()
        var itemNum = 1
        for (g in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[g]
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (t in 0 until group.length) {
                    val format = group.getTrackFormat(t)
                    val lang = format.language?.uppercase() ?: "UND"
                    val label = format.label ?: "Subtitles $itemNum ($lang)"
                    list.add(
                        TrackItem(
                            groupIndex = g,
                            trackIndex = t,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(t)
                        )
                    )
                    itemNum++
                }
            }
        }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.75f)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Audiotrack else Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = AccentSky,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Audio & Subtitle Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Tab Switcher (Audio / Subtitles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) PrimaryBlue else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio (${audioTracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) PrimaryBlue else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subtitles (${subtitleTracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Track Selection List
                val activeList = if (selectedTab == 0) audioTracks else subtitleTracks

                if (selectedTab == 1) {
                    // Subtitles Off Option
                    val isSubtitlesDisabled = subtitleTracks.none { it.isSelected }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFocused) CardSurfaceFocused else if (isSubtitlesDisabled) PrimaryBlue.copy(alpha = 0.2f) else CardSurface)
                            .border(1.dp, if (isFocused) FocusRingColor else BorderSubtle, RoundedCornerShape(8.dp))
                            .focusable(interactionSource = interactionSource)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                player.trackSelectionParameters = player.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Off (Disable Subtitles)",
                            color = if (isSubtitlesDisabled) AccentSky else TextPrimary,
                            fontWeight = if (isSubtitlesDisabled) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        if (isSubtitlesDisabled) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentSky, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (activeList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "No alternate audio tracks in stream." else "No embedded subtitle tracks available.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(activeList) { _, track ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) CardSurfaceFocused else if (track.isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardSurface)
                                    .border(1.dp, if (isFocused) FocusRingColor else BorderSubtle, RoundedCornerShape(8.dp))
                                    .focusable(interactionSource = interactionSource)
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        val trackGroup = currentTracks.groups[track.groupIndex].mediaTrackGroup
                                        val override = TrackSelectionOverride(trackGroup, listOf(track.trackIndex))
                                        val trackType = if (selectedTab == 0) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
                                        player.trackSelectionParameters = player.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(trackType, false)
                                            .setOverrideForType(override)
                                            .build()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    color = if (track.isSelected) AccentSky else TextPrimary,
                                    fontWeight = if (track.isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )

                                if (track.isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AccentSky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
