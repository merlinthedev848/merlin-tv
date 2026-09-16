package com.example.merlinmedia

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.merlinmedia.data.CatalogRepository
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.data.NetworkMonitor
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.ui.dialogs.SettingsDialog
import com.example.merlinmedia.ui.dialogs.UpdateDialog
import com.example.merlinmedia.ui.screens.HomeScreen
import com.example.merlinmedia.ui.screens.PlayerScreen
import com.example.merlinmedia.ui.screens.SplashScreen
import com.example.merlinmedia.ui.theme.MerlinTvTheme
import com.example.merlinmedia.updater.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var favoritesManager: FavoritesManager
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesManager = FavoritesManager(this)
        networkMonitor = NetworkMonitor(this)

        setContent {
            MerlinTvTheme {
                MerlinTvApp(
                    favoritesManager = favoritesManager,
                    networkMonitor = networkMonitor
                )
            }
        }
    }
}

@Composable
fun MerlinTvApp(
    favoritesManager: FavoritesManager,
    networkMonitor: NetworkMonitor
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    var liveChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var movieChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var seriesChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var activePlaylist by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }

    fun loadChannels(forceRefresh: Boolean = false) {
        isLoading = true
        coroutineScope.launch {
            val (live, movies, series) = CatalogRepository.loadAllCatalogs(context, forceRefresh = forceRefresh)
            liveChannels = live
            movieChannels = movies
            seriesChannels = series
            isLoading = false
        }
    }

    // Initial load & silent update check
    LaunchedEffect(Unit) {
        loadChannels()

        // Silent background update check
        coroutineScope.launch(Dispatchers.IO) {
            val updateResult = UpdateManager.checkForUpdates()
            updateResult.onSuccess { info ->
                if (info != null) {
                    withContext(Dispatchers.Main) {
                        availableUpdate = info
                        Toast.makeText(
                            context,
                            "Merlin TV update v${info.version} available! Tap 'Update' to install.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Main navigation router
    if (selectedItem != null) {
        // Player bypasses splash entirely
        PlayerScreen(
            initialItem = selectedItem!!,
            playlist = activePlaylist.ifEmpty { listOf(selectedItem!!) },
            favoritesManager = favoritesManager,
            onBack = { selectedItem = null }
        )
    } else {
        // Show wizard splash only on cold start (channels not yet loaded)
        val showSplash = isLoading && liveChannels.isEmpty() && movieChannels.isEmpty()
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = {
                // Splash fades out slowly; home screen fades in
                fadeIn(animationSpec = tween(600)) togetherWith
                    fadeOut(animationSpec = tween(400))
            },
            label = "splashToHome"
        ) { isSplashVisible ->
            if (isSplashVisible) {
                SplashScreen()
            } else {
                HomeScreen(
                    liveChannels = liveChannels,
                    movieChannels = movieChannels,
                    seriesChannels = seriesChannels,
                    isLoading = isLoading,
                    isOnline = isOnline,
                    availableUpdate = availableUpdate,
                    favoritesManager = favoritesManager,
                    onSelectChannel = { item, playlist ->
                        selectedItem = item
                        activePlaylist = playlist
                    },
                    onOpenUpdateDialog = {
                        showUpdateDialog = true
                    },
                    onOpenSettingsDialog = {
                        showSettingsDialog = true
                    },
                    onRefreshChannels = {
                        loadChannels(forceRefresh = true)
                    }
                )
            }
        }
    }

    // Update Dialog
    if (showUpdateDialog) {
        UpdateDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }

    // Settings & Custom Playlist Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onPlaylistChanged = {
                loadChannels(forceRefresh = true)
            }
        )
    }
}