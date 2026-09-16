package com.example.merlinmedia.data

import android.content.Context
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    private const val FREE_TV_PLAYLIST = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
    private const val MOVIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/movies.m3u"
    private const val CLASSIC_MOVIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/classic.m3u"
    private const val SERIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/series.m3u"
    private const val ANIMATION_SERIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/animation.m3u"
    private const val DOCUMENTARY_PLAYLIST = "https://iptv-org.github.io/iptv/categories/documentary.m3u"

    @Volatile
    private var cachedLiveChannels: List<MediaEntry> = emptyList()

    @Volatile
    private var cachedMovieChannels: List<MediaEntry> = emptyList()

    @Volatile
    private var cachedSeriesChannels: List<MediaEntry> = emptyList()

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

    private fun fetchM3uContent(url: String): String {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android TV; MerlinTV)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }
        }.getOrDefault("")
    }

    /**
     * Load dynamic Live TV channels from selected country feeds, Free-TV curated lists, and custom playlists.
     */
    suspend fun loadLive(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedLiveChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedLiveChannels
        }

        val allEntries = mutableListOf<MediaEntry>()
        val activeCountries = context?.let { getSelectedCountryCodes(it) } ?: setOf("UK", "USA")

        // 1. Fetch active country playlists in parallel
        coroutineScope {
            val countryTasks = availableCountries
                .filter { activeCountries.contains(it.first) }
                .map { (country, url) ->
                    async(Dispatchers.IO) {
                        val body = fetchM3uContent(url)
                        if (body.isNotBlank()) {
                            M3uParser.parse(body, defaultCountry = country, defaultKind = Kind.LIVE, sourceLabel = "iptv-org $country")
                        } else {
                            emptyList()
                        }
                    }
                }

            // 2. Fetch curated Free-TV global playlist
            val freeTvTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(FREE_TV_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.LIVE, sourceLabel = "Free-TV")
                } else {
                    emptyList()
                }
            }

            // 3. Fetch custom playlists if configured
            val customTasks = context?.let { ctx ->
                getCustomPlaylists(ctx).map { (name, url) ->
                    async(Dispatchers.IO) {
                        val body = fetchM3uContent(url)
                        if (body.isNotBlank()) {
                            M3uParser.parse(body, defaultCountry = "Custom", defaultKind = Kind.LIVE, sourceLabel = name)
                        } else {
                            emptyList()
                        }
                    }
                }
            } ?: emptyList()

            // Collect all results
            countryTasks.forEach { allEntries.addAll(it.await()) }
            allEntries.addAll(freeTvTask.await())
            customTasks.forEach { allEntries.addAll(it.await()) }
        }

        val distinctList = allEntries.distinctBy { it.url }
        cachedLiveChannels = distinctList
        distinctList
    }

    /**
     * Load dynamic real Movies catalog from public legal M3U movie channels & classic cinema streams.
     */
    suspend fun loadMovies(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedMovieChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedMovieChannels
        }

        val allEntries = mutableListOf<MediaEntry>()

        coroutineScope {
            val moviesTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(MOVIES_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Movies", defaultKind = Kind.MOVIE, sourceLabel = "Public Cinema")
                } else emptyList()
            }

            val classicTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(CLASSIC_MOVIES_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Classic", defaultKind = Kind.MOVIE, sourceLabel = "Classic Movies")
                } else emptyList()
            }

            allEntries.addAll(moviesTask.await())
            allEntries.addAll(classicTask.await())
        }

        val distinctList = allEntries.distinctBy { it.url }
        cachedMovieChannels = distinctList
        distinctList
    }

    /**
     * Load dynamic real TV Series & Animation catalog from public legal M3U streams.
     */
    suspend fun loadSeries(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedSeriesChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedSeriesChannels
        }

        val allEntries = mutableListOf<MediaEntry>()

        coroutineScope {
            val seriesTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(SERIES_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Series", defaultKind = Kind.SERIES, sourceLabel = "Public Series")
                } else emptyList()
            }

            val animationTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(ANIMATION_SERIES_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Animation", defaultKind = Kind.SERIES, sourceLabel = "Animation")
                } else emptyList()
            }

            val docTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(DOCUMENTARY_PLAYLIST)
                if (body.isNotBlank()) {
                    M3uParser.parse(body, defaultCountry = "Documentary", defaultKind = Kind.SERIES, sourceLabel = "Documentaries")
                } else emptyList()
            }

            allEntries.addAll(seriesTask.await())
            allEntries.addAll(animationTask.await())
            allEntries.addAll(docTask.await())
        }

        val distinctList = allEntries.distinctBy { it.url }
        cachedSeriesChannels = distinctList
        distinctList
    }

    /**
     * Parallel loader for all three catalogs simultaneously.
     */
    suspend fun loadAllCatalogs(context: Context? = null, forceRefresh: Boolean = false): Triple<List<MediaEntry>, List<MediaEntry>, List<MediaEntry>> = coroutineScope {
        val liveDeferred = async(Dispatchers.IO) { loadLive(context, forceRefresh) }
        val moviesDeferred = async(Dispatchers.IO) { loadMovies(context, forceRefresh) }
        val seriesDeferred = async(Dispatchers.IO) { loadSeries(context, forceRefresh) }

        Triple(liveDeferred.await(), moviesDeferred.await(), seriesDeferred.await())
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
}