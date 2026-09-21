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

    override fun onDestroy() {
        super.onDestroy()
        favoritesManager.close()
    }
}

@Composable
fun MerlinTvApp(
    favoritesManager: FavoritesManager,
    networkMonitor: NetworkMonitor
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyConnected())

    var liveChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var plutoChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var skyChannels by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
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
            val (live, pluto, sky, movies, series) = CatalogRepository.loadAllCatalogs(context, forceRefresh = forceRefresh)
            liveChannels = live
            plutoChannels = pluto
            skyChannels = sky
            movieChannels = movies
            seriesChannels = series
            isLoading = false
        }
    }

    // Initial load & silent update check
    LaunchedEffect(Unit) {
        loadChannels()

        // Background update check
        coroutineScope.launch(Dispatchers.IO) {
            val updateResult = UpdateManager.checkForUpdates(context)
            updateResult.onSuccess { info ->
                if (info != null) {
                    withContext(Dispatchers.Main) {
                        availableUpdate = info
                        Toast.makeText(
                            context,
                            "Merlin TV update v${info.version} available! Open Settings/Updates to install.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Auto-reload when connection is restored if lists are empty
    LaunchedEffect(isOnline) {
        if (isOnline && (liveChannels.isEmpty() && plutoChannels.isEmpty() && skyChannels.isEmpty())) {
            loadChannels(forceRefresh = true)
        }
    }

    // Main navigation router
    if (selectedItem != null) {
        PlayerScreen(
            initialItem = selectedItem!!,
            playlist = activePlaylist.ifEmpty { listOf(selectedItem!!) },
            favoritesManager = favoritesManager,
            onBack = { selectedItem = null }
        )
    } else {
        val showSplash = isLoading && liveChannels.isEmpty() && plutoChannels.isEmpty() && skyChannels.isEmpty() && movieChannels.isEmpty()
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = {
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
                    plutoChannels = plutoChannels,
                    skyChannels = skyChannels,
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