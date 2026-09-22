package com.example.merlinmedia.data

import android.content.Context
import android.content.SharedPreferences
import com.example.merlinmedia.data.local.AppDatabase
import com.example.merlinmedia.data.local.entity.FavoriteEntity
import com.example.merlinmedia.data.local.entity.RecentItemEntity
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.io.Closeable

class FavoritesManager(context: Context) : Closeable {
    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val favoritesDao = database.favoritesDao()
    private val recentHistoryDao = database.recentHistoryDao()

    private val prefs: SharedPreferences = context.getSharedPreferences("merlin_favorites_prefs", Context.MODE_PRIVATE)
    private val managerJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + managerJob)

    companion object {
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_RECENT_HISTORY = "recent_history_json"
        private const val KEY_MIGRATED_TO_ROOM = "migrated_to_room_v1"
        private const val MAX_HISTORY = 40
    }

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<MediaEntry>>(emptyList())
    val recentHistory: StateFlow<List<MediaEntry>> = _recentHistory.asStateFlow()

    private val lock = Any()

    init {
        // 1. Initial fast memory seeding from SharedPreferences (for instant UI responsiveness)
        synchronized(lock) {
            val storedFavs = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            _favoriteIds.value = HashSet(storedFavs)

            val rawHistory = prefs.getString(KEY_RECENT_HISTORY, null)
            if (!rawHistory.isNullOrBlank()) {
                val list = mutableListOf<MediaEntry>()
                runCatching {
                    val jsonArray = JSONArray(rawHistory)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            MediaEntry(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                url = obj.optString("url"),
                                type = runCatching { Kind.valueOf(obj.optString("type")) }.getOrDefault(Kind.LIVE),
                                country = obj.optString("country"),
                                group = obj.optString("group"),
                                logo = obj.optString("logo").ifBlank { null },
                                description = obj.optString("description"),
                                source = obj.optString("source"),
                                year = obj.optString("year"),
                                duration = obj.optString("duration"),
                                genre = obj.optString("genre"),
                                rating = obj.optString("rating"),
                                season = if (obj.has("season")) obj.optInt("season") else null,
                                episode = if (obj.has("episode")) obj.optInt("episode") else null,
                                backdrop = obj.optString("backdrop").ifBlank { null },
                                isVod = obj.optBoolean("isVod", false),
                                quality = obj.optString("quality", "1080p"),
                                tvgId = obj.optString("tvgId")
                            )
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e, "Failed to parse initial recent history from prefs")
                }
                _recentHistory.value = list
            }
        }

        // 2. Perform one-time migration to Room if not yet done
        ioScope.launch {
            runCatching {
                migrateFromPrefsToRoomIfNeeded()
            }.onFailure { e ->
                Timber.e(e, "Error migrating prefs to Room")
            }

            // 3. Observe Room database changes reactively
            launch {
                favoritesDao.getAllFavoriteIdsFlow().collectLatest { ids ->
                    synchronized(lock) {
                        _favoriteIds.value = ids.toSet()
                    }
                }
            }

            launch {
                recentHistoryDao.getRecentHistoryFlow(MAX_HISTORY).collectLatest { entities ->
                    val entries = entities.map { it.toMediaEntry() }
                    synchronized(lock) {
                        _recentHistory.value = entries
                    }
                }
            }
        }
    }

    private suspend fun migrateFromPrefsToRoomIfNeeded() {
        val isMigrated = prefs.getBoolean(KEY_MIGRATED_TO_ROOM, false)
        if (!isMigrated) {
            val storedFavs = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            if (storedFavs.isNotEmpty()) {
                val entities = storedFavs.map { FavoriteEntity(id = it) }
                favoritesDao.insertFavorites(entities)
            }

            val initialHistory = _recentHistory.value
            if (initialHistory.isNotEmpty()) {
                val entities = initialHistory.map { RecentItemEntity.fromMediaEntry(it) }
                recentHistoryDao.insertRecentItems(entities)
            }

            prefs.edit().putBoolean(KEY_MIGRATED_TO_ROOM, true).apply()
            Timber.d("Successfully migrated favorites and history to Room DB")
        }
    }

    fun getFavoriteIds(): Set<String> {
        return _favoriteIds.value
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(id: String): Boolean {
        val isCurrentlyFav = isFavorite(id)
        if (isCurrentlyFav) {
            removeFavorite(id)
            return false
        } else {
            addFavorite(id)
            return true
        }
    }

    fun addFavorite(id: String) {
        synchronized(lock) {
            val current = HashSet(_favoriteIds.value)
            if (current.add(id)) {
                _favoriteIds.value = current
            }
        }
        ioScope.launch {
            runCatching {
                favoritesDao.insertFavorite(FavoriteEntity(id = id))
            }.onFailure { e ->
                Timber.e(e, "Failed to insert favorite: $id")
            }
        }
    }

    fun removeFavorite(id: String) {
        synchronized(lock) {
            val current = HashSet(_favoriteIds.value)
            if (current.remove(id)) {
                _favoriteIds.value = current
            }
        }
        ioScope.launch {
            runCatching {
                favoritesDao.deleteFavoriteById(id)
            }.onFailure { e ->
                Timber.e(e, "Failed to delete favorite: $id")
            }
        }
    }

    fun addToRecent(entry: MediaEntry) {
        synchronized(lock) {
            val current = _recentHistory.value.toMutableList()
            current.removeAll { it.url == entry.url || (it.id.isNotBlank() && it.id == entry.id) }
            current.add(0, entry)
            while (current.size > MAX_HISTORY) {
                current.removeAt(current.size - 1)
            }
            _recentHistory.value = current
        }
        ioScope.launch {
            runCatching {
                val entity = RecentItemEntity.fromMediaEntry(entry)
                recentHistoryDao.insertRecentItem(entity)
                recentHistoryDao.trimRecentHistory(MAX_HISTORY)
            }.onFailure { e ->
                Timber.e(e, "Failed to insert recent item: ${entry.title}")
            }
        }
    }

    fun getRecentHistory(): List<MediaEntry> {
        return _recentHistory.value
    }

    fun clearRecentHistory() {
        synchronized(lock) {
            _recentHistory.value = emptyList()
        }
        ioScope.launch {
            runCatching {
                recentHistoryDao.clearAllRecentHistory()
            }.onFailure { e ->
                Timber.e(e, "Failed to clear recent history")
            }
        }
    }

    override fun close() {
        managerJob.cancel()
    }
}