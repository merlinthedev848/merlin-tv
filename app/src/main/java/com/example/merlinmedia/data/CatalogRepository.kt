package com.example.merlinmedia.data

import android.content.Context
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object CatalogRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val availableCountries = listOf(
        "UK" to "https://iptv-org.github.io/iptv/countries/uk.m3u",
        "USA" to "https://iptv-org.github.io/iptv/countries/us.m3u",
        "Canada" to "https://iptv-org.github.io/iptv/countries/ca.m3u",
        "Australia" to "https://iptv-org.github.io/iptv/countries/au.m3u",
        "France" to "https://iptv-org.github.io/iptv/countries/fr.m3u",
        "Germany" to "https://iptv-org.github.io/iptv/countries/de.m3u",
        "Spain" to "https://iptv-org.github.io/iptv/countries/es.m3u",
        "Italy" to "https://iptv-org.github.io/iptv/countries/it.m3u"
    )

    private var cachedLiveChannels: List<MediaEntry> = emptyList()

    fun getSelectedCountryCodes(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("merlin_country_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("selected_countries", setOf("UK", "USA")) ?: setOf("UK", "USA")
    }

    fun setSelectedCountryCodes(context: Context, countries: Set<String>) {
        context.getSharedPreferences("merlin_country_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("selected_countries", countries)
            .apply()
        cachedLiveChannels = emptyList() // Invalidate cache
    }

    suspend fun loadLive(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedLiveChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedLiveChannels
        }

        val allEntries = mutableListOf<MediaEntry>()
        val activeCountries = context?.let { getSelectedCountryCodes(it) } ?: setOf("UK", "USA")

        // 1. Load active country playlists
        for ((country, url) in availableCountries) {
            if (activeCountries.contains(country)) {
                runCatching {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android TV; MerlinTV)")
                        .build()
                    val body = client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.string().orEmpty() else ""
                    }
                    if (body.isNotBlank()) {
                        allEntries.addAll(M3uParser.parse(body, defaultCountry = country, sourceLabel = "iptv-org $country"))
                    }
                }
            }
        }

        // 2. Load custom playlists if configured in SharedPreferences
        context?.let { ctx ->
            val customUrls = getCustomPlaylists(ctx)
            for ((name, url) in customUrls) {
                runCatching {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android TV; MerlinTV)")
                        .build()
                    val body = client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body?.string().orEmpty() else ""
                    }
                    if (body.isNotBlank()) {
                        allEntries.addAll(M3uParser.parse(body, defaultCountry = "Custom", sourceLabel = name))
                    }
                }
            }
        }

        val distinctList = allEntries.distinctBy { it.url }
        cachedLiveChannels = distinctList
        distinctList
    }

    fun getCategories(entries: List<MediaEntry>): List<String> {
        val groups = entries.map { it.group.trim() }
            .filter { it.isNotBlank() && !it.equals("undefined", ignoreCase = true) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        return listOf("All") + groups
    }

    fun getCustomPlaylists(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences("merlin_playlists", Context.MODE_PRIVATE)
        val raw = prefs.getString("custom_urls", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { item ->
            val parts = item.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }

    fun addCustomPlaylist(context: Context, name: String, url: String) {
        val current = getCustomPlaylists(context).toMutableList()
        current.add(name to url)
        val encoded = current.joinToString(";") { "${it.first}|${it.second}" }
        context.getSharedPreferences("merlin_playlists", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_urls", encoded)
            .apply()
        cachedLiveChannels = emptyList() // invalidate cache
    }

    fun removeCustomPlaylist(context: Context, url: String) {
        val current = getCustomPlaylists(context).filterNot { it.second == url }
        val encoded = current.joinToString(";") { "${it.first}|${it.second}" }
        context.getSharedPreferences("merlin_playlists", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_urls", encoded)
            .apply()
        cachedLiveChannels = emptyList() // invalidate cache
    }

    val movies: List<MediaEntry> = listOf(
        MediaEntry(
            id = "movie-bbb",
            title = "Big Buck Bunny",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            type = Kind.MOVIE,
            group = "Animation",
            country = "Open Movie",
            logo = "https://peach.blender.org/wp-content/uploads/bbb-splash.png",
            description = "A large and lovable rabbit deals with bullying forest creatures in this classic Blender Foundation open animation.",
            source = "Blender Open Project"
        ),
        MediaEntry(
            id = "movie-sintel",
            title = "Sintel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            type = Kind.MOVIE,
            group = "Fantasy",
            country = "Open Movie",
            logo = "https://durian.blender.org/wp-content/themes/durian/images/header/header_sintel.jpg",
            description = "A lonely young woman named Sintel searches for a baby dragon she befriended and named Scales.",
            source = "Blender Foundation"
        ),
        MediaEntry(
            id = "movie-tears-of-steel",
            title = "Tears of Steel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            type = Kind.MOVIE,
            group = "Sci-Fi",
            country = "Open Movie",
            logo = "https://mango.blender.org/wp-content/uploads/2012/09/poster_tos_large.jpg",
            description = "Set in a dystopian future in Amsterdam, a group of scientists attempt to stage a critical moment in time.",
            source = "Blender Foundation"
        ),
        MediaEntry(
            id = "movie-elephants-dream",
            title = "Elephants Dream",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            type = Kind.MOVIE,
            group = "Sci-Fi",
            country = "Open Movie",
            logo = "https://orange.blender.org/wp-content/themes/orange/images/header.jpg",
            description = "The world's first open movie, following Proog and Emo on a journey through the surreal machine.",
            source = "Blender Foundation"
        )
    )

    val series: List<MediaEntry> = listOf(
        MediaEntry(
            id = "series-blazes",
            title = "Open Shorts · Ep. 1 · For Bigger Blazes",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            type = Kind.SERIES,
            group = "Demo Shorts",
            country = "US",
            description = "High-definition chromecast and streaming demonstration showcase short.",
            source = "Google Open Media"
        ),
        MediaEntry(
            id = "series-escapes",
            title = "Open Shorts · Ep. 2 · For Bigger Escapes",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            type = Kind.SERIES,
            group = "Demo Shorts",
            country = "US",
            description = "Action-packed outdoor showcase clip exploring dynamic frame rates and HDR color grading.",
            source = "Google Open Media"
        ),
        MediaEntry(
            id = "series-fun",
            title = "Open Shorts · Ep. 3 · For Bigger Fun",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            type = Kind.SERIES,
            group = "Demo Shorts",
            country = "US",
            description = "Vibrant demo short highlighting high-resolution cinematic pacing and surround sound fidelity.",
            source = "Google Open Media"
        ),
        MediaEntry(
            id = "series-joyrides",
            title = "Open Shorts · Ep. 4 · For Bigger Joyrides",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            type = Kind.SERIES,
            group = "Demo Shorts",
            country = "US",
            description = "Scenic landscape and aerial motion sequence testing bitrate adaptability.",
            source = "Google Open Media"
        )
    )
}