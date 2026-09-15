package com.example.merlinmedia.data

import android.content.Context
import android.content.SharedPreferences
import com.example.merlinmedia.model.MediaEntry
import org.json.JSONArray
import org.json.JSONObject

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("merlin_favorites_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_RECENT_HISTORY = "recent_history_json"
        private const val MAX_HISTORY = 30
    }

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun isFavorite(id: String): Boolean {
        return getFavoriteIds().contains(id)
    }

    fun toggleFavorite(id: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val newState = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return newState
    }

    fun addFavorite(id: String) {
        val current = getFavoriteIds().toMutableSet()
        if (current.add(id)) {
            prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        }
    }

    fun removeFavorite(id: String) {
        val current = getFavoriteIds().toMutableSet()
        if (current.remove(id)) {
            prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        }
    }

    fun addToRecent(entry: MediaEntry) {
        val list = getRecentHistory().toMutableList()
        list.removeAll { it.url == entry.url }
        list.add(0, entry)
        val trimmed = list.take(MAX_HISTORY)
        
        val jsonArray = JSONArray()
        for (item in trimmed) {
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

    fun getRecentHistory(): List<MediaEntry> {
        val raw = prefs.getString(KEY_RECENT_HISTORY, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<MediaEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
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
            list
        }.getOrDefault(emptyList())
    }
}