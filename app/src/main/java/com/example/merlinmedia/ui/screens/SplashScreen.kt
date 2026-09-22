package com.example.merlinmedia.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.merlinmedia.R
import com.example.merlinmedia.ui.theme.AccentSky
import com.example.merlinmedia.ui.theme.PrimaryBlue
import com.example.merlinmedia.ui.theme.TextPrimary
import com.example.merlinmedia.ui.theme.TextSecondary

@Composable
fun SplashScreen(
    loadingStatus: String = "Loading Merlin TV Catalogs & Streams...",
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var hasVideoError by remember { mutableStateOf(false) }

    val introPlayer = remember {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.merlintv_intro}")
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
        }.getOrNull()
    }

    DisposableEffect(introPlayer) {
        onDispose {
            introPlayer?.stop()
            introPlayer?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onSkip?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (introPlayer != null && !hasVideoError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = introPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Animated Fallback Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "Merlin TV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "High-Definition Broadcast & On-Demand IPTV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Loading Progress Overlay at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color = AccentSky,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = loadingStatus,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
