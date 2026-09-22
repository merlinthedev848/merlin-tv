package com.example.merlinmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.merlinmedia.ui.MainViewModel
import com.example.merlinmedia.ui.dialogs.SettingsDialog
import com.example.merlinmedia.ui.dialogs.UpdateDialog
import com.example.merlinmedia.ui.screens.HomeScreen
import com.example.merlinmedia.ui.screens.PlayerScreen
import com.example.merlinmedia.ui.screens.SplashScreen
import com.example.merlinmedia.ui.theme.MerlinTvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MerlinTvTheme {
                MerlinTvApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MerlinTvApp(viewModel: MainViewModel) {

    // Collect all state from ViewModel
    val liveChannels by viewModel.liveChannels.collectAsStateWithLifecycle()
    val plutoChannels by viewModel.plutoChannels.collectAsStateWithLifecycle()
    val skyChannels by viewModel.skyChannels.collectAsStateWithLifecycle()
    val movieChannels by viewModel.movieChannels.collectAsStateWithLifecycle()
    val seriesChannels by viewModel.seriesChannels.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSplashDone by viewModel.isSplashDone.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Auto-reload when connection is restored if lists are empty
    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.autoReloadIfEmpty()
        }
    }

    // Main navigation router
    val currentSelectedItem = selectedItem
    if (currentSelectedItem != null) {
        PlayerScreen(
            initialItem = currentSelectedItem,
            playlist = activePlaylist.ifEmpty { listOf(currentSelectedItem) },
            favoritesManager = viewModel.favoritesManager,
            onBack = { viewModel.clearSelection() }
        )
    } else {
        val showSplash = !isSplashDone
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                    fadeOut(animationSpec = tween(400))
            },
            label = "splashToHome"
        ) { isSplashVisible ->
            if (isSplashVisible) {
                SplashScreen(
                    onSkip = { viewModel.skipSplash() }
                )
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
                    favoritesManager = viewModel.favoritesManager,
                    onSelectChannel = { item, playlist ->
                        viewModel.selectItem(item, playlist)
                    },
                    onOpenUpdateDialog = {
                        showUpdateDialog = true
                    },
                    onOpenSettingsDialog = {
                        showSettingsDialog = true
                    },
                    onRefreshChannels = {
                        viewModel.loadChannels(forceRefresh = true)
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
                viewModel.loadChannels(forceRefresh = true)
            },
            onCheckForUpdates = {
                showUpdateDialog = true
            }
        )
    }
}