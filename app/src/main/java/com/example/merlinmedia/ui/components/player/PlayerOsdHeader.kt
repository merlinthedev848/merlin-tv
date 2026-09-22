package com.example.merlinmedia.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.ui.components.QualityBadge
import com.example.merlinmedia.ui.theme.*

@Composable
fun PlayerOsdHeader(
    currentItem: MediaEntry,
    isFavorite: Boolean,
    sleepTimerRemainingMinutes: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenAudioSubtitles: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMiniGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SolidBarBg)
            .border(1.dp, BorderSubtle)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Column {
                    Text(
                        text = currentItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentItem.type == Kind.LIVE || currentItem.type == Kind.PLUTO || currentItem.type == Kind.SKY) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LiveBadgeColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        QualityBadge(quality = currentItem.quality.ifBlank { "1080p" })
                        if (currentItem.country.isNotBlank()) {
                            Text(currentItem.country, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (currentItem.group.isNotBlank()) {
                            Text("·  ${currentItem.group}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio & Subtitle Selector Button
                Button(
                    onClick = onOpenAudioSubtitles,
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subtitles", tint = AccentSky, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audio / CC", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Sleep Timer Indicator/Button
                Button(
                    onClick = onOpenSleepTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sleepTimerRemainingMinutes > 0) AccentGold.copy(alpha = 0.2f) else CardSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (sleepTimerRemainingMinutes > 0) AccentGold else BorderSubtle)
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerRemainingMinutes > 0) AccentGold else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sleepTimerRemainingMinutes > 0) "${sleepTimerRemainingMinutes}m" else "Sleep Timer",
                        color = if (sleepTimerRemainingMinutes > 0) AccentGold else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mini Guide Toggle Button
                Button(
                    onClick = onOpenMiniGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Channel Guide", tint = AccentSky, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guide", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Favorite Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentGold else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
