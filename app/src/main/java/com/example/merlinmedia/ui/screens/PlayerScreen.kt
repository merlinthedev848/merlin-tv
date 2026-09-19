package com.example.merlinmedia.ui.screens

import android.app.Activity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.model.AspectRatioMode
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.player.ExoPlayerHelper
import com.example.merlinmedia.ui.components.ChannelGridCard
import com.example.merlinmedia.ui.components.TvSearchBar
import com.example.merlinmedia.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    initialItem: MediaEntry,
    playlist: List<MediaEntry>,
    favoritesManager: FavoritesManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentItem by remember { mutableStateOf(initialItem) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var showMiniGuide by remember { mutableStateOf(false) }
    var miniGuideSearch by remember { mutableStateOf("") }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    // Fix #2: don't initialise once from initialItem — derive from currentItem on every change
    var isFavorite by remember { mutableStateOf(false) }
    var reconnectAttempt by remember { mutableIntStateOf(0) }
    var isAutoReconnecting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currentIndex = remember(currentItem, playlist) {
        val idx = playlist.indexOfFirst { it.url == currentItem.url }
        if (idx >= 0) idx else 0
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-hide controls after 5 seconds if mini guide is not open
    LaunchedEffect(showControls, isPlaying, showMiniGuide) {
        if (showControls && isPlaying && errorMessage == null && !showMiniGuide && !isAutoReconnecting) {
            delay(5000)
            showControls = false
        }
    }

    // Record to recent history and sync isFavorite immediately on channel change
    LaunchedEffect(currentItem) {
        favoritesManager.addToRecent(currentItem)
        isFavorite = favoritesManager.isFavorite(currentItem.id)
    }

    // Initialize ExoPlayer via ExoPlayerHelper
    val player = remember {
        ExoPlayerHelper.createPlayer(context, lowLatencyMode = true)
    }

    fun playItem(item: MediaEntry, resetRetry: Boolean = true) {
        currentItem = item
        errorMessage = null
        isBuffering = true
        if (resetRetry) {
            reconnectAttempt = 0
            isAutoReconnecting = false
        }
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(ExoPlayerHelper.buildMediaItem(item.url, item.title))
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(Unit) {
        playItem(currentItem)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        errorMessage = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        reconnectAttempt = 0
                        isAutoReconnecting = false
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                if (reconnectAttempt < 3) {
                    val nextAttempt = reconnectAttempt + 1
                    reconnectAttempt = nextAttempt
                    isAutoReconnecting = true
                    errorMessage = null
                    coroutineScope.launch {
                        delay(2000)
                        if (isAutoReconnecting) {
                            playItem(currentItem, resetRetry = false)
                        }
                    }
                } else {
                    isAutoReconnecting = false
                    errorMessage = "Stream disconnected: ${error.message ?: "Broadcast offline or geo-restricted"}"
                    showControls = true
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Multi-level Back Handler
    BackHandler {
        if (showMiniGuide) {
            showMiniGuide = false
        } else if (showControls) {
            showControls = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (!showControls && !showMiniGuide) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            when {
                                // Fix #6: Only jump straight to mini guide when OSD is already hidden.
                                // If controls are visible, first close them (next UP press opens guide).
                                !showMiniGuide && !showControls -> {
                                    showMiniGuide = true
                                    true
                                }
                                !showMiniGuide && showControls -> {
                                    // Let focus navigate within OSD; don't intercept
                                    false
                                }
                                else -> false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (playlist.size > 1 && !showMiniGuide) {
                                val prevIdx = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
                                playItem(playlist[prevIdx])
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (playlist.size > 1 && !showMiniGuide) {
                                val nextIdx = (currentIndex + 1) % playlist.size
                                playItem(playlist[nextIdx])
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (player.isPlaying) player.pause() else player.play()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            player.play()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            player.pause()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (showMiniGuide) {
                    showMiniGuide = false
                } else {
                    showControls = !showControls
                }
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = when (aspectMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                }
            },
            update = { view ->
                view.resizeMode = when (aspectMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering / Reconnecting Indicator
        if ((isBuffering || isAutoReconnecting) && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                            text = if (isAutoReconnecting) "Reconnecting stream (Attempt $reconnectAttempt of 3)..." else "Loading Stream...",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Error Recovery Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                    modifier = Modifier.padding(32.dp).widthIn(max = 500.dp)
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
                            text = errorMessage ?: "The broadcast source is currently offline or unreachable.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { playItem(currentItem) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }

                            if (playlist.size > 1) {
                                Button(
                                    onClick = {
                                        val nextIdx = (currentIndex + 1) % playlist.size
                                        playItem(playlist[nextIdx])
                                    },
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

        // Solid, Clean On-Screen Display (OSD)
        AnimatedVisibility(
            visible = showControls && !showMiniGuide,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Header Solid Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
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
                                    if (currentItem.type == Kind.LIVE) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(LiveBadgeColor)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
                            // Mini Guide Toggle Button
                            Button(
                                onClick = { showMiniGuide = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Channel Guide", tint = AccentSky, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Channel Guide", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Favorite Button
                            IconButton(
                                onClick = {
                                    val newState = favoritesManager.toggleFavorite(currentItem.id)
                                    isFavorite = newState
                                },
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

                // Bottom Controls Solid Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(SolidBarBg)
                        .border(1.dp, BorderSubtle)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Surfing Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlist.size > 1) {
                                IconButton(
                                    onClick = {
                                        val prevIdx = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
                                        playItem(playlist[prevIdx])
                                    },
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
                                onClick = {
                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                },
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

                            if (playlist.size > 1) {
                                IconButton(
                                    onClick = {
                                        val nextIdx = (currentIndex + 1) % playlist.size
                                        playItem(playlist[nextIdx])
                                    },
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

                        // Info & Aspect Ratio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (playlist.size > 1) {
                                Text(
                                    text = "Channel ${currentIndex + 1} of ${playlist.size}",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    aspectMode = when (aspectMode) {
                                        AspectRatioMode.FIT -> AspectRatioMode.ZOOM
                                        AspectRatioMode.ZOOM -> AspectRatioMode.STRETCH
                                        AspectRatioMode.STRETCH -> AspectRatioMode.FIT
                                    }
                                },
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
        }

        // Solid, Opaque Mini Channel Guide Drawer
        AnimatedVisibility(
            visible = showMiniGuide,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(SolidBarBg)
                    .border(1.dp, BorderSubtle)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Channel Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentSky
                        )
                        IconButton(onClick = { showMiniGuide = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    TvSearchBar(
                        query = miniGuideSearch,
                        onQueryChange = { miniGuideSearch = it },
                        placeholderText = "Quick find channel..."
                    )

                    val filteredMiniList = remember(miniGuideSearch, playlist) {
                        if (miniGuideSearch.isBlank()) playlist
                        else {
                            val q = miniGuideSearch.trim().lowercase()
                            playlist.filter {
                                it.title.lowercase().contains(q) || it.group.lowercase().contains(q)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredMiniList, key = { index, it -> "${it.id}-${it.url}-$index" }) { index, item ->
                            ChannelGridCard(
                                item = item,
                                channelNumber = 101 + index,
                                isFavorite = favoritesManager.isFavorite(item.id),
                                onToggleFavorite = {
                                    val newState = favoritesManager.toggleFavorite(item.id)
                                    // Fix #16: sync OSD header star if we toggled the current item
                                    if (item.id == currentItem.id) isFavorite = newState
                                },
                                onFocusChange = {},
                                onClick = {
                                    playItem(item)
                                    showMiniGuide = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}