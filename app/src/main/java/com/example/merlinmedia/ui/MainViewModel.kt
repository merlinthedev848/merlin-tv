package com.example.merlinmedia.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.merlinmedia.data.AllCatalogsResult
import com.example.merlinmedia.data.CatalogRepository
import com.example.merlinmedia.data.FavoritesManager
import com.example.merlinmedia.data.NetworkMonitor
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.updater.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    val favoritesManager: FavoritesManager,
    val networkMonitor: NetworkMonitor
) : AndroidViewModel(application) {

    // ── Catalog State ──
    private val instantCatalogs: AllCatalogsResult =
        CatalogRepository.getInstantCatalogs(application)

    private val _liveChannels = MutableStateFlow(instantCatalogs.live)
    val liveChannels: StateFlow<List<MediaEntry>> = _liveChannels.asStateFlow()

    private val _plutoChannels = MutableStateFlow(instantCatalogs.pluto)
    val plutoChannels: StateFlow<List<MediaEntry>> = _plutoChannels.asStateFlow()

    private val _skyChannels = MutableStateFlow(instantCatalogs.sky)
    val skyChannels: StateFlow<List<MediaEntry>> = _skyChannels.asStateFlow()

    private val _movieChannels = MutableStateFlow(instantCatalogs.movies)
    val movieChannels: StateFlow<List<MediaEntry>> = _movieChannels.asStateFlow()

    private val _seriesChannels = MutableStateFlow(instantCatalogs.series)
    val seriesChannels: StateFlow<List<MediaEntry>> = _seriesChannels.asStateFlow()

    // ── UI State ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSplashDone = MutableStateFlow(false)
    val isSplashDone: StateFlow<Boolean> = _isSplashDone.asStateFlow()

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    // ── Navigation State ──
    private val _selectedItem = MutableStateFlow<MediaEntry?>(null)
    val selectedItem: StateFlow<MediaEntry?> = _selectedItem.asStateFlow()

    private val _activePlaylist = MutableStateFlow<List<MediaEntry>>(emptyList())
    val activePlaylist: StateFlow<List<MediaEntry>> = _activePlaylist.asStateFlow()

    // ── Network ──
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isCurrentlyConnected())

    init {
        // Splash timer
        viewModelScope.launch {
            delay(2000L)
            _isSplashDone.value = true
        }

        // Initial load
        loadChannels(forceRefresh = false)

        // Background update check
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val updateResult = UpdateManager.checkForUpdates(ctx)
            updateResult.onSuccess { info ->
                if (info != null) {
                    withContext(Dispatchers.Main) {
                        _availableUpdate.value = info
                        Toast.makeText(
                            ctx,
                            "Merlin TV update v${info.version} available! Open Settings/Updates to install.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val result = CatalogRepository.loadAllCatalogs(ctx, forceRefresh = forceRefresh)
                if (result.live.isNotEmpty()) _liveChannels.value = result.live
                if (result.pluto.isNotEmpty()) _plutoChannels.value = result.pluto
                if (result.sky.isNotEmpty()) _skyChannels.value = result.sky
                if (result.movies.isNotEmpty()) _movieChannels.value = result.movies
                if (result.series.isNotEmpty()) _seriesChannels.value = result.series
            } catch (e: Throwable) {
                Timber.e(e, "Failed to load catalogs")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun skipSplash() {
        _isSplashDone.value = true
    }

    fun selectItem(item: MediaEntry, playlist: List<MediaEntry>) {
        _selectedItem.value = item
        _activePlaylist.value = playlist
    }

    fun clearSelection() {
        _selectedItem.value = null
        _activePlaylist.value = emptyList()
    }

    fun autoReloadIfEmpty() {
        val hasData = _liveChannels.value.isNotEmpty() ||
            _plutoChannels.value.isNotEmpty() ||
            _skyChannels.value.isNotEmpty()
        if (!hasData) {
            loadChannels(forceRefresh = true)
        }
    }
}
