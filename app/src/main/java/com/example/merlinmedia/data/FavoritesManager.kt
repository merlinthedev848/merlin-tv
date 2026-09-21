package com.example.merlinmedia.data

import android.content.Context
import android.content.SharedPreferences
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable

class FavoritesManager(context: Context) : Closeable {
    private val prefs: SharedPreferences = context.getSharedPreferences("merlin_favorites_prefs", Context.MODE_PRIVATE)
    private val managerJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + managerJob)

    companion object {
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_RECENT_HISTORY = "recent_history_json"
        private const val MAX_HISTORY = 40
    }

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<MediaEntry>>(emptyList())
    val recentHistory: StateFlow<List<MediaEntry>> = _recentHistory.asStateFlow()

    private val lock = Any()

    init {
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
                }
                _recentHistory.value = list
            }
        }
    }

    fun getFavoriteIds(): Set<String> {
        return _favoriteIds.value
    }

    fun isFavorite(id: String): Boolean {
        return _favoriteIds.value.contains(id)
    }

    fun toggleFavorite(id: String): Boolean {
        val newState: Boolean
        val updated: Set<String>
        synchronized(lock) {
            val current = HashSet(_favoriteIds.value)
            if (current.contains(id)) {
                current.remove(id)
                newState = false
            } else {
                current.add(id)
                newState = true
            }
            updated = current
            _favoriteIds.value = updated
        }
        persistFavoritesAsync(updated)
        return newState
    }

    fun addFavorite(id: String) {
        val updated: Set<String>
        synchronized(lock) {
            val current = HashSet(_favoriteIds.value)
            if (current.add(id)) {
                updated = current
                _favoriteIds.value = updated
            } else {
                return
            }
        }
        persistFavoritesAsync(updated)
    }

    fun removeFavorite(id: String) {
        val updated: Set<String>
        synchronized(lock) {
            val current = HashSet(_favoriteIds.value)
            if (current.remove(id)) {
                updated = current
                _favoriteIds.value = updated
            } else {
                return
            }
        }
        persistFavoritesAsync(updated)
    }

    private fun persistFavoritesAsync(snapshot: Set<String>) {
        ioScope.launch {
            prefs.edit().putStringSet(KEY_FAVORITES, snapshot).apply()
        }
    }

    fun addToRecent(entry: MediaEntry) {
        val updated: List<MediaEntry>
        synchronized(lock) {
            val current = _recentHistory.value.toMutableList()
            current.removeAll { it.url == entry.url || (it.id.isNotBlank() && it.id == entry.id) }
            current.add(0, entry)
            while (current.size > MAX_HISTORY) {
                current.removeAt(current.size - 1)
            }
            updated = current
            _recentHistory.value = updated
        }
        persistRecentHistoryAsync(updated)
    }

    fun getRecentHistory(): List<MediaEntry> {
        return _recentHistory.value
    }

    private fun persistRecentHistoryAsync(snapshot: List<MediaEntry>) {
        ioScope.launch {
            runCatching {
                val jsonArray = JSONArray()
                for (item in snapshot) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("url", item.url)
                        put("type", item.type.name)
                        put("country", item.country)
                        put("group", item.group)
                        put("logo", item.logo ?: "")
                        put("description", item.description)
                        put("source", item.source)
                        put("year", item.year)
                        put("duration", item.duration)
                        put("genre", item.genre)
                        put("rating", item.rating)
                        if (item.season != null) put("season", item.season)
                        if (item.episode != null) put("episode", item.episode)
                        put("backdrop", item.backdrop ?: "")
                        put("isVod", item.isVod)
                        put("quality", item.quality)
                        put("tvgId", item.tvgId)
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString(KEY_RECENT_HISTORY, jsonArray.toString()).apply()
            }
        }
    }

    override fun close() {
        managerJob.cancel()
    }
}