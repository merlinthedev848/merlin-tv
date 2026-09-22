package com.example.merlinmedia.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.merlinmedia.ui.theme.*

@Composable
fun NumericChannelJumpOverlay(
    channelInput: String,
    modifier: Modifier = Modifier
) {
    if (channelInput.isEmpty()) return

    Box(
        modifier = modifier
            .padding(top = 24.dp, end = 32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(2.dp, AccentSky, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ch. ", color = AccentSky, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(channelInput, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
        }
    }
}

@Composable
fun BufferingOrReconnectingOverlay(
    isBuffering: Boolean,
    isAutoReconnecting: Boolean,
    reconnectAttempt: Int,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    if ((!isBuffering && !isAutoReconnecting) || errorMessage != null) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SolidBarBg),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(
                    color = if (isAutoReconnecting) AccentGold else AccentSky,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isAutoReconnecting) "Reconnecting stream (Attempt $reconnectAttempt of 3)..." else "Buffering stream...",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun StreamErrorRecoveryOverlay(
    errorMessage: String?,
    hasNextChannel: Boolean,
    onRetry: () -> Unit,
    onNextChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (errorMessage == null) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                Text(
                    text = "Stream Unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = errorMessage,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }

                    if (hasNextChannel) {
                        Button(
                            onClick = onNextChannel,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Next Channel", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
