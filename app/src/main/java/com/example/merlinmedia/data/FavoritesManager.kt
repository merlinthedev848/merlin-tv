package com.example.merlinmedia.data

import android.content.Context
import android.content.SharedPreferences
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("merlin_favorites_prefs", Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_RECENT_HISTORY = "recent_history_json"
        private const val MAX_HISTORY = 40
    }

    // High-performance in-memory caches for O(1) instantaneous UI access
    private val cachedFavoriteIds: MutableSet<String> = HashSet()
    private val cachedRecentHistory: MutableList<MediaEntry> = ArrayList()
    private val lock = Any()

    init {
        synchronized(lock) {
            // Preload favorite IDs
            val storedFavs = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            cachedFavoriteIds.addAll(storedFavs)

            // Preload recent history
            val rawHistory = prefs.getString(KEY_RECENT_HISTORY, null)
            if (!rawHistory.isNullOrBlank()) {
                runCatching {
                    val jsonArray = JSONArray(rawHistory)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        cachedRecentHistory.add(
                            MediaEntry(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                url = obj.optString("url"),
                                type = runCatching { com.example.merlinmedia.model.Kind.valueOf(obj.optString("type")) }.getOrDefault(com.example.merlinmedia.model.Kind.LIVE),
                                country = obj.optString("country"),
                                group = obj.optString("group"),
                                logo = obj.optString("logo").ifBlank { null },
                                description = obj.optString("description"),
                                source = obj.optString("source")
                            )
                        )
                    }
                }
            }
        }
    }

    fun getFavoriteIds(): Set<String> {
        synchronized(lock) {
            return HashSet(cachedFavoriteIds)
        }
    }

    fun isFavorite(id: String): Boolean {
        synchronized(lock) {
            return cachedFavoriteIds.contains(id)
        }
    }

    fun toggleFavorite(id: String): Boolean {
        val newState: Boolean
        val snapshot: Set<String>
        synchronized(lock) {
            if (cachedFavoriteIds.contains(id)) {
                cachedFavoriteIds.remove(id)
                newState = false
            } else {
                cachedFavoriteIds.add(id)
                newState = true
            }
            snapshot = HashSet(cachedFavoriteIds)
        }
        persistFavoritesAsync(snapshot)
        return newState
    }

    fun addFavorite(id: String) {
        val snapshot: Set<String>
        synchronized(lock) {
            if (cachedFavoriteIds.add(id)) {
                snapshot = HashSet(cachedFavoriteIds)
            } else {
                return
            }
        }
        persistFavoritesAsync(snapshot)
    }

    fun removeFavorite(id: String) {
        val snapshot: Set<String>
        synchronized(lock) {
            if (cachedFavoriteIds.remove(id)) {
                snapshot = HashSet(cachedFavoriteIds)
            } else {
                return
            }
        }
        persistFavoritesAsync(snapshot)
    }

    private fun persistFavoritesAsync(snapshot: Set<String>) {
        ioScope.launch {
            prefs.edit().putStringSet(KEY_FAVORITES, snapshot).apply()
        }
    }

    fun addToRecent(entry: MediaEntry) {
        val snapshot: List<MediaEntry>
        synchronized(lock) {
            cachedRecentHistory.removeAll { it.url == entry.url }
            cachedRecentHistory.add(0, entry)
            while (cachedRecentHistory.size > MAX_HISTORY) {
                cachedRecentHistory.removeAt(cachedRecentHistory.size - 1)
            }
            snapshot = ArrayList(cachedRecentHistory)
        }
        persistRecentHistoryAsync(snapshot)
    }

    fun getRecentHistory(): List<MediaEntry> {
        synchronized(lock) {
            return ArrayList(cachedRecentHistory)
        }
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
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString(KEY_RECENT_HISTORY, jsonArray.toString()).apply()
            }
        }
    }
}