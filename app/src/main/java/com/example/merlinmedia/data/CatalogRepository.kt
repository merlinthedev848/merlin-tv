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
        val defaultAll = availableCountries.map { it.first }.toSet()
        return prefs.getStringSet("selected_countries", defaultAll) ?: defaultAll
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
        // Always include curated verified channels first for guaranteed immediate playback
        allEntries.addAll(curatedLiveChannels)

        val activeCountries = context?.let { getSelectedCountryCodes(it) } ?: availableCountries.map { it.first }.toSet()

        // 1. Fetch active country playlists in parallel
        coroutineScope {
            val countryTasks = availableCountries
                .filter { activeCountries.contains(it.first) }
                .map { (country, url) ->
                    async(Dispatchers.IO) {
                        val body = fetchM3uContent(url)
                        if (body.isNotBlank()) {
                            val parsed = M3uParser.parse(body, defaultCountry = country, defaultKind = Kind.LIVE, sourceLabel = "iptv-org $country")
                            parsed.map { entry ->
                                val formattedGroup = when {
                                    entry.group.contains("|") -> entry.group.trim()
                                    entry.group.isNotBlank() && !entry.group.equals("General", ignoreCase = true) && !entry.group.equals("undefined", ignoreCase = true) ->
                                        "$country | ${entry.group.trim().uppercase()}"
                                    else -> "$country | GENERAL"
                                }
                                entry.copy(group = formattedGroup)
                            }
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

    // High uptime, verified working UK & International Live Streams with complete metadata
    val curatedLiveChannels: List<MediaEntry> = listOf(
        MediaEntry(
            id = "uk-bbc-news-fhd",
            title = "BBC News (1080p FHD)",
            url = "https://vs-hls-push-uk-live.akamaized.net/x=4/i=urn:bbc:pips:service:bbc_news_channel_hd/t=3840/v=pv14/b=5070016/main.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/BBC_News_2022.svg/320px-BBC_News_2022.svg.png",
            description = "24-hour news and current affairs from the British Broadcasting Corporation in Full HD.",
            source = "BBC Broadcast"
        ),
        MediaEntry(
            id = "uk-sky-news-hd",
            title = "Sky News UK (1080p FHD)",
            url = "https://skynews-live.akamaized.net/hls/live/2026859/skynews_hd/master.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/9/90/Sky_News_logo_2020.svg/320px-Sky_News_logo_2020.svg.png",
            description = "Live breaking news, video, analysis and headline reports from the UK and around the world.",
            source = "Sky UK"
        ),
        MediaEntry(
            id = "uk-bloomberg-hd",
            title = "Bloomberg TV Europe (1080p FHD)",
            url = "https://liveproduseast.global.ssl.fastly.net/us/Channel-HD-AWS-virginia-1/live.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Bloomberg_Television_logo.svg/320px-Bloomberg_Television_logo.svg.png",
            description = "Global business and financial news, stock market updates, and economic reports.",
            source = "Bloomberg"
        ),
        MediaEntry(
            id = "uk-euronews-hd",
            title = "Euronews English (1080p FHD)",
            url = "https://euronews-euronews-world-1-gb.samsung.wurl.tv/playlist.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Euronews_2016_logo.svg/320px-Euronews_2016_logo.svg.png",
            description = "All the latest international headlines, European perspective and in-depth reporting.",
            source = "Euronews"
        ),
        MediaEntry(
            id = "uk-france24-en",
            title = "France 24 English (1080p FHD)",
            url = "https://f24hls-i.akamaihd.net/hls/live/221193/F24_EN_LO_HLS/master_2000.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/France_24_logo.svg/320px-France_24_logo.svg.png",
            description = "International news 24/7 with a European and French perspective.",
            source = "France Medias Monde"
        ),
        MediaEntry(
            id = "uk-dw-english",
            title = "DW English (1080p FHD)",
            url = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_logo.svg/320px-Deutsche_Welle_logo.svg.png",
            description = "Deutsche Welle international broadcast channel delivering news, culture, and science.",
            source = "Deutsche Welle"
        ),
        MediaEntry(
            id = "uk-redbull-tv",
            title = "Red Bull TV (1080p FHD)",
            url = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            type = Kind.LIVE,
            group = "UK | SPORTS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png",
            description = "Action sports, live events, music festivals, documentaries and inspiring films.",
            source = "Red Bull Media House"
        ),
        MediaEntry(
            id = "uk-nasa-tv-hd",
            title = "NASA TV HD (1080p FHD)",
            url = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            type = Kind.LIVE,
            group = "UK | ENTERTAINMENT",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/320px-NASA_logo.svg.png",
            description = "Live views from the International Space Station, rocket launches, and space missions.",
            source = "NASA"
        ),
        MediaEntry(
            id = "uk-reuters-live",
            title = "Reuters TV Live (1080p FHD)",
            url = "https://reuters-reutersnow-1-us.rakuten.wurl.tv/playlist.m3u8",
            type = Kind.LIVE,
            group = "UK | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Reuters_Logo.svg/320px-Reuters_Logo.svg.png",
            description = "Real-time global news reporting from journalists stationed worldwide.",
            source = "Thomson Reuters"
        ),
        MediaEntry(
            id = "us-abc-news-live",
            title = "ABC News Live (1080p FHD)",
            url = "https://content.uplynk.com/channel/3324f2467c414329b3b0cc5cd987b6be.m3u8",
            type = Kind.LIVE,
            group = "USA | NEWS",
            country = "USA",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/ABC_News_logo_2021.svg/320px-ABC_News_logo_2021.svg.png",
            description = "24/7 streaming news from ABC News featuring live breaking news and special reports.",
            source = "ABC News"
        ),
        MediaEntry(
            id = "us-cbs-news-live",
            title = "CBS News 24/7 (1080p FHD)",
            url = "https://cbsn-us.cbsnstream.cbsnews.com/out/v1/55a8648e8f134e82a470f83d562de701/master.m3u8",
            type = Kind.LIVE,
            group = "USA | NEWS",
            country = "USA",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/CBS_News_logo_2020.svg/320px-CBS_News_logo_2020.svg.png",
            description = "Live news broadcasts, original reporting and analysis from CBS News journalists.",
            source = "Paramount Global"
        ),
        MediaEntry(
            id = "us-live-now-fox",
            title = "LiveNOW from FOX (1080p FHD)",
            url = "https://livenow-fox-hls.amagi.tv/playlist.m3u8",
            type = Kind.LIVE,
            group = "USA | NEWS",
            country = "USA",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/LiveNOW_from_Fox_logo.svg/320px-LiveNOW_from_Fox_logo.svg.png",
            description = "Raw, unfiltered live breaking news events from across the United States.",
            source = "FOX Television Stations"
        ),
        MediaEntry(
            id = "us-weather-nation",
            title = "WeatherNation (1080p FHD)",
            url = "https://weathernation.tubi.video/hls/live.m3u8",
            type = Kind.LIVE,
            group = "USA | GENERAL",
            country = "USA",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/e/e0/WeatherNation_logo.svg/320px-WeatherNation_logo.svg.png",
            description = "Continuous live weather radar, forecasts and severe weather storm coverage.",
            source = "WeatherNation"
        )
    )
}