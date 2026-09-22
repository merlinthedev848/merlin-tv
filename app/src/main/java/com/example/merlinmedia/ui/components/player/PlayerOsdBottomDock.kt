package com.example.merlinmedia.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.AspectRatioMode
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.ui.theme.*

@Composable
fun PlayerOsdBottomDock(
    currentItem: MediaEntry,
    isPlaying: Boolean,
    currentIndex: Int,
    playlistSize: Int,
    playbackSpeed: Float,
    aspectMode: AspectRatioMode,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SolidBarBg)
            .border(1.dp, BorderSubtle)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause and Skip Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (playlistSize > 1) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Channel", tint = TextPrimary)
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (playlistSize > 1) {
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next Channel", tint = TextPrimary)
                    }
                }
            }

            // Playback Speed (for VOD items) and Aspect Ratio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentItem.isVod) {
                    OutlinedButton(
                        onClick = onCycleSpeed,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface, contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${playbackSpeed}x", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (playlistSize > 1) {
                    Text(
                        text = "Channel ${currentIndex + 1} of $playlistSize",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedButton(
                    onClick = onCycleAspectRatio,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(aspectMode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
