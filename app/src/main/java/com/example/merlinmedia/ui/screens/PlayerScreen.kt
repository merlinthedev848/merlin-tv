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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.model.AspectRatioMode
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.player.ExoPlayerHelper
import com.example.merlinmedia.ui.components.player.BufferingOrReconnectingOverlay
import com.example.merlinmedia.ui.components.player.NumericChannelJumpOverlay
import com.example.merlinmedia.ui.components.player.PlayerMiniGuideDrawer
import com.example.merlinmedia.ui.components.player.PlayerOsdBottomDock
import com.example.merlinmedia.ui.components.player.PlayerOsdHeader
import com.example.merlinmedia.ui.components.player.StreamErrorRecoveryOverlay
import com.example.merlinmedia.ui.dialogs.AudioAndSubtitleDialog
import com.example.merlinmedia.ui.dialogs.SleepTimerDialog
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
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioSubtitlesDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingMinutes by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var reconnectAttempt by remember { mutableIntStateOf(0) }
    var isAutoReconnecting by remember { mutableStateOf(false) }

    // Numeric Keypad Input State
    var numericChannelInput by remember { mutableStateOf("") }
    var numericKeyTimestamp by remember { mutableLongStateOf(0L) }

    val coroutineScope = rememberCoroutineScope()
    val favoriteIds by favoritesManager.favoriteIds.collectAsState()
    val isFavorite = favoriteIds.contains(currentItem.id)

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

    // Auto-hide controls after 5 seconds if mini guide / dialogs are not open
    LaunchedEffect(showControls, isPlaying, showMiniGuide, showSleepTimerDialog, showAudioSubtitlesDialog) {
        if (showControls && isPlaying && errorMessage == null && !showMiniGuide && !showSleepTimerDialog && !showAudioSubtitlesDialog && !isAutoReconnecting) {
            delay(5000)
            showControls = false
        }
    }

    // Initialize ExoPlayer
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
        ExoPlayerHelper.setPlaybackSpeed(player, playbackSpeed)
        player.prepare()
        player.playWhenReady = true
    }

    // Numeric keypad auto-jump timer (1.2s timeout)
    LaunchedEffect(numericKeyTimestamp) {
        if (numericChannelInput.isNotEmpty()) {
            delay(1200L)
            val channelNumber = numericChannelInput.toIntOrNull()
            if (channelNumber != null) {
                val targetIndex = (channelNumber - 101).coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
                if (targetIndex in playlist.indices) {
                    playItem(playlist[targetIndex])
                }
            }
            numericChannelInput = ""
        }
    }

    // Sleep Timer countdown ticker
    LaunchedEffect(sleepTimerRemainingMinutes) {
        if (sleepTimerRemainingMinutes > 0) {
            while (sleepTimerRemainingMinutes > 0) {
                delay(60_000L)
                sleepTimerRemainingMinutes -= 1
                if (sleepTimerRemainingMinutes <= 0) {
                    isPlaying = false
                    player.pause()
                }
            }
        }
    }

    // Record to recent history immediately on channel change
    LaunchedEffect(currentItem) {
        favoritesManager.addToRecent(currentItem)
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
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
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
        if (showAudioSubtitlesDialog) {
            showAudioSubtitlesDialog = false
        } else if (showSleepTimerDialog) {
            showSleepTimerDialog = false
        } else if (showMiniGuide) {
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
                    val keyCode = event.nativeKeyEvent.keyCode
                    val digit = when (keyCode) {
                        KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
                        KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
                        KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
                        KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
                        KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
                        KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
                        KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
                        KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
                        KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
                        KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
                        else -> null
                    }

                    if (digit != null && !showMiniGuide) {
                        if (numericChannelInput.length < 4) {
                            numericChannelInput += digit
                            numericKeyTimestamp = System.currentTimeMillis()
                        }
                        return@onKeyEvent true
                    }

                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (numericChannelInput.isNotEmpty()) {
                                val channelNumber = numericChannelInput.toIntOrNull()
                                if (channelNumber != null) {
                                    val targetIndex = (channelNumber - 101).coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
                                    if (targetIndex in playlist.indices) {
                                        playItem(playlist[targetIndex])
                                    }
                                }
                                numericChannelInput = ""
                                true
                            } else if (!showControls && !showMiniGuide) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            when {
                                !showMiniGuide && !showControls -> {
                                    showMiniGuide = true
                                    true
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

        // Numeric Keypad Quick Jump Overlay
        NumericChannelJumpOverlay(
            channelInput = numericChannelInput,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Buffering / Reconnecting Indicator
        BufferingOrReconnectingOverlay(
            isBuffering = isBuffering,
            isAutoReconnecting = isAutoReconnecting,
            reconnectAttempt = reconnectAttempt,
            errorMessage = errorMessage
        )

        // Error Recovery Overlay
        StreamErrorRecoveryOverlay(
            errorMessage = errorMessage,
            hasNextChannel = playlist.size > 1,
            onRetry = { playItem(currentItem) },
            onNextChannel = {
                val nextIdx = (currentIndex + 1) % playlist.size
                playItem(playlist[nextIdx])
            }
        )

        // On-Screen Display (OSD)
        AnimatedVisibility(
            visible = showControls && !showMiniGuide,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerOsdHeader(
                    currentItem = currentItem,
                    isFavorite = isFavorite,
                    sleepTimerRemainingMinutes = sleepTimerRemainingMinutes,
                    onBack = onBack,
                    onToggleFavorite = { favoritesManager.toggleFavorite(currentItem.id) },
                    onOpenAudioSubtitles = { showAudioSubtitlesDialog = true },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onOpenMiniGuide = { showMiniGuide = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                PlayerOsdBottomDock(
                    currentItem = currentItem,
                    isPlaying = isPlaying,
                    currentIndex = currentIndex,
                    playlistSize = playlist.size,
                    playbackSpeed = playbackSpeed,
                    aspectMode = aspectMode,
                    onTogglePlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onPrevious = {
                        val prevIdx = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
                        playItem(playlist[prevIdx])
                    },
                    onNext = {
                        val nextIdx = (currentIndex + 1) % playlist.size
                        playItem(playlist[nextIdx])
                    },
                    onCycleSpeed = {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                        playbackSpeed = speeds[nextIdx]
                        ExoPlayerHelper.setPlaybackSpeed(player, playbackSpeed)
                    },
                    onCycleAspectRatio = {
                        aspectMode = when (aspectMode) {
                            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
                            AspectRatioMode.ZOOM -> AspectRatioMode.STRETCH
                            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Quick Channel Guide Drawer
        AnimatedVisibility(
            visible = showMiniGuide,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(200))
        ) {
            PlayerMiniGuideDrawer(
                playlist = playlist,
                currentItem = currentItem,
                isFavorite = { favoritesManager.isFavorite(it) },
                onToggleFavorite = { favoritesManager.toggleFavorite(it) },
                onSelectItem = { playItem(it) },
                onClose = { showMiniGuide = false }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentRemainingMinutes = sleepTimerRemainingMinutes,
                onSetTimer = { mins -> sleepTimerRemainingMinutes = mins },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        // Audio & Subtitles Dialog
        if (showAudioSubtitlesDialog) {
            AudioAndSubtitleDialog(
                player = player,
                onDismiss = { showAudioSubtitlesDialog = false }
            )
        }
    }
}