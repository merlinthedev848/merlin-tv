package com.example.merlinmedia.data

import android.content.Context
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import timber.log.Timber

data class AllCatalogsResult(
    val live: List<MediaEntry>,
    val pluto: List<MediaEntry>,
    val sky: List<MediaEntry>,
    val movies: List<MediaEntry>,
    val series: List<MediaEntry>
)

object CatalogRepository {

    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    /** Bounded dispatcher limiting concurrent network calls to 4 threads */
    private val boundedIO = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    val availableCountries = listOf(
        "UK" to "https://iptv-org.github.io/iptv/countries/uk.m3u",
        "USA" to "https://iptv-org.github.io/iptv/countries/us.m3u",
        "Canada" to "https://iptv-org.github.io/iptv/countries/ca.m3u",
        "Australia" to "https://iptv-org.github.io/iptv/countries/au.m3u",
        "New Zealand" to "https://iptv-org.github.io/iptv/countries/nz.m3u",
        "Ireland" to "https://iptv-org.github.io/iptv/countries/ie.m3u",
        "France" to "https://iptv-org.github.io/iptv/countries/fr.m3u",
        "Germany" to "https://iptv-org.github.io/iptv/countries/de.m3u",
        "Italy" to "https://iptv-org.github.io/iptv/countries/it.m3u",
        "Spain" to "https://iptv-org.github.io/iptv/countries/es.m3u",
        "Portugal" to "https://iptv-org.github.io/iptv/countries/pt.m3u",
        "Netherlands" to "https://iptv-org.github.io/iptv/countries/nl.m3u",
        "South Africa" to "https://iptv-org.github.io/iptv/countries/za.m3u",
        "India" to "https://iptv-org.github.io/iptv/countries/in.m3u",
        "Japan" to "https://iptv-org.github.io/iptv/countries/jp.m3u",
        "South Korea" to "https://iptv-org.github.io/iptv/countries/kr.m3u",
        "Philippines" to "https://iptv-org.github.io/iptv/countries/ph.m3u",
        "Brazil" to "https://iptv-org.github.io/iptv/countries/br.m3u",
        "Mexico" to "https://iptv-org.github.io/iptv/countries/mx.m3u"
    )

    // FAST & Provider Feeds
    private const val PLUTO_ALL_PLAYLIST = "https://raw.githubusercontent.com/BuddyChewChew/app-m3u-generator/refs/heads/main/playlists/plutotv_all.m3u"
    private const val SAMSUNG_ALL_PLAYLIST = "https://raw.githubusercontent.com/BuddyChewChew/app-m3u-generator/refs/heads/main/playlists/samsungtvplus_all.m3u"
    private const val XIAOMI_PLAYLIST = "https://www.apsattv.com/xiaomi.m3u"
    private const val RAKUTEN_UK_PLAYLIST = "https://www.apsattv.com/rakutentv-uk.m3u"
    private const val FREE_TV_PLAYLIST = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
    private const val PLEX_ALL_PLAYLIST = "https://i.mjh.nz/Plex/all.m3u8"
    const val PLEX_EPG_URL = "https://i.mjh.nz/Plex/all.xml"
    private const val PLUTO_GB_PLAYLIST = "https://raw.githubusercontent.com/BuddyChewChew/pluto/main/pluto_gb.m3u"
    private const val PLUTO_US_PLAYLIST = "https://raw.githubusercontent.com/BuddyChewChew/pluto/main/pluto_us.m3u"

    // Categorized Feeds
    private const val MOVIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/movies.m3u"
    private const val NEWS_PLAYLIST = "https://iptv-org.github.io/iptv/categories/news.m3u"
    private const val DOCUMENTARY_PLAYLIST = "https://iptv-org.github.io/iptv/categories/documentary.m3u"
    private const val MUSIC_PLAYLIST = "https://iptv-org.github.io/iptv/categories/music.m3u"
    private const val COMEDY_PLAYLIST = "https://iptv-org.github.io/iptv/categories/comedy.m3u"
    private const val SPORTS_PLAYLIST = "https://iptv-org.github.io/iptv/categories/sports.m3u"
    private const val CLASSIC_MOVIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/classic.m3u"
    private const val SERIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/series.m3u"
    private const val ENTERTAINMENT_PLAYLIST = "https://iptv-org.github.io/iptv/categories/entertainment.m3u"
    private const val KIDS_PLAYLIST = "https://iptv-org.github.io/iptv/categories/kids.m3u"
    private const val ANIMATION_SERIES_PLAYLIST = "https://iptv-org.github.io/iptv/categories/animation.m3u"
    private const val AUTO_PLAYLIST = "https://iptv-org.github.io/iptv/categories/auto.m3u"

    @Volatile private var cachedLiveChannels: List<MediaEntry> = emptyList()
    @Volatile private var cachedPlutoChannels: List<MediaEntry> = emptyList()
    @Volatile private var cachedSkyChannels: List<MediaEntry> = emptyList()
    @Volatile private var cachedMovieChannels: List<MediaEntry> = emptyList()
    @Volatile private var cachedSeriesChannels: List<MediaEntry> = emptyList()

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
        cachedLiveChannels = emptyList()
    }

    private fun fetchM3uContent(context: Context?, url: String): String {
        return runCatching {
            val client = HttpClientProvider.getClient(context)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android TV; MerlinTV)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }
        }.getOrDefault("")
    }

    fun detectQuality(raw: String, defaultQuality: String = "1080p"): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("4k") || lower.contains("uhd") || lower.contains("2160p") -> "4K"
            lower.contains("1080p") || lower.contains("1080") || lower.contains("fhd") -> "1080p"
            lower.contains("720p") || lower.contains("720") || lower.contains("hd") -> "720p"
            lower.contains("576p") || lower.contains("480p") || lower.contains("sd") -> "SD"
            else -> defaultQuality
        }
    }

    // ==========================================
    // DISK CACHING WITH TTL FOR INSTANT OFFLINE
    // ==========================================
    private fun getCacheFile(context: Context, categoryName: String): File {
        val cacheDir = File(context.cacheDir, "catalogs")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return File(cacheDir, "$categoryName.json")
    }

    private fun saveToDiskCache(context: Context, categoryName: String, items: List<MediaEntry>) {
        runCatching {
            val file = getCacheFile(context, categoryName)
            val root = JSONObject()
            root.put("timestamp", System.currentTimeMillis())
            val jsonArray = JSONArray()
            items.forEach { entry ->
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("url", entry.url)
                    put("type", entry.type.name)
                    put("country", entry.country)
                    put("group", entry.group)
                    put("logo", entry.logo ?: "")
                    put("description", entry.description)
                    put("source", entry.source)
                    put("year", entry.year)
                    put("duration", entry.duration)
                    put("genre", entry.genre)
                    put("rating", entry.rating)
                    if (entry.season != null) put("season", entry.season)
                    if (entry.episode != null) put("episode", entry.episode)
                    put("backdrop", entry.backdrop ?: "")
                    put("isVod", entry.isVod)
                    put("quality", entry.quality)
                    put("tvgId", entry.tvgId)
                }
                jsonArray.put(obj)
            }
            root.put("items", jsonArray)
            file.writeText(root.toString())
        }
    }

    private fun loadFromDiskCache(context: Context, categoryName: String, ignoreTtl: Boolean = false): List<MediaEntry> {
        return runCatching {
            val file = getCacheFile(context, categoryName)
            if (!file.exists()) return emptyList()
            val content = file.readText()
            if (content.isBlank()) return emptyList()

            val jsonArray: JSONArray
            if (content.trim().startsWith("{")) {
                val root = JSONObject(content)
                val timestamp = root.optLong("timestamp", 0L)
                if (!ignoreTtl && (System.currentTimeMillis() - timestamp > CACHE_TTL_MS)) {
                    return emptyList()
                }
                jsonArray = root.optJSONArray("items") ?: return emptyList()
            } else {
                jsonArray = JSONArray(content)
            }

            val list = mutableListOf<MediaEntry>()
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
            list
        }.getOrDefault(emptyList())
    }

    /**
     * Load dynamic Live TV channels from selected country feeds, Free-TV curated lists, and custom playlists.
     */
    suspend fun loadLive(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedLiveChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedLiveChannels
        }

        if (!forceRefresh && context != null) {
            val diskItems = loadFromDiskCache(context, "live")
            if (diskItems.isNotEmpty()) {
                cachedLiveChannels = diskItems
                return@withContext diskItems
            }
        }

        val allEntries = Collections.synchronizedList(mutableListOf<MediaEntry>())
        allEntries.addAll(curatedLiveChannels)

        val activeCountries = context?.let { getSelectedCountryCodes(it) } ?: availableCountries.map { it.first }.toSet()

        coroutineScope {
            val countryTasks = availableCountries
                .filter { activeCountries.contains(it.first) }
                .map { (country, url) ->
                    async(Dispatchers.IO) {
                        val body = fetchM3uContent(context, url)
                        if (body.isNotBlank()) {
                            val parsed = M3uParser.parse(body, defaultCountry = country, defaultKind = Kind.LIVE, sourceLabel = "iptv-org $country")
                            parsed.map { entry ->
                                val formattedGroup = when {
                                    entry.group.contains("|") -> entry.group.trim()
                                    entry.group.isNotBlank() && !entry.group.equals("General", ignoreCase = true) && !entry.group.equals("undefined", ignoreCase = true) ->
                                        "$country | ${entry.group.trim().uppercase()}"
                                    else -> "$country | GENERAL"
                                }
                                entry.copy(country = country, group = formattedGroup)
                            }
                        } else {
                            emptyList()
                        }
                    }
                }

            val sportsTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, SPORTS_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Sports", defaultKind = Kind.LIVE, sourceLabel = "iptv-org Sports")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Sports | ${entry.group.trim().uppercase()}" else "Sports | LIVE ACTION"
                        entry.copy(group = grp, country = entry.country.ifBlank { "Sports" })
                    }
                } else emptyList()
            }

            val newsTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, NEWS_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "News", defaultKind = Kind.LIVE, sourceLabel = "iptv-org News")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "News | ${entry.group.trim().uppercase()}" else "News | LIVE NEWS"
                        entry.copy(group = grp, country = entry.country.ifBlank { "News" })
                    }
                } else emptyList()
            }

            val documentaryTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, DOCUMENTARY_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Docs", defaultKind = Kind.LIVE, sourceLabel = "iptv-org Docs")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Documentary | ${entry.group.trim().uppercase()}" else "Documentary | NATURE & SCIENCE"
                        entry.copy(group = grp, country = entry.country.ifBlank { "Global" })
                    }
                } else emptyList()
            }

            val musicTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, MUSIC_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Music", defaultKind = Kind.LIVE, sourceLabel = "iptv-org Music")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Music | ${entry.group.trim().uppercase()}" else "Music | 24/7 HITS"
                        entry.copy(group = grp, country = entry.country.ifBlank { "Global" })
                    }
                } else emptyList()
            }

            val comedyTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, COMEDY_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Comedy", defaultKind = Kind.LIVE, sourceLabel = "iptv-org Comedy")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Comedy | ${entry.group.trim().uppercase()}" else "Comedy | SITCOMS & HUMOUR"
                        entry.copy(group = grp, country = entry.country.ifBlank { "Global" })
                    }
                } else emptyList()
            }

            val autoTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, AUTO_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Auto", defaultKind = Kind.LIVE, sourceLabel = "iptv-org Auto")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Auto | ${entry.group.trim().uppercase()}" else "Auto | MOTORSPORTS"
                        entry.copy(group = grp, country = entry.country.ifBlank { "Auto" })
                    }
                } else emptyList()
            }

            val freeTvTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, FREE_TV_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.LIVE, sourceLabel = "Free-TV")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank() && !entry.group.equals("General", ignoreCase = true))
                            "Free-TV | ${entry.group.trim().uppercase()}"
                        else "Free-TV | BROADCAST"
                        entry.copy(group = grp)
                    }
                } else {
                    emptyList()
                }
            }

            val customTasks = context?.let { ctx ->
                getCustomPlaylists(ctx).map { (name, url) ->
                    async(Dispatchers.IO) {
                        val body = fetchM3uContent(ctx, url)
                        if (body.isNotBlank()) {
                            M3uParser.parse(body, defaultCountry = "Custom", defaultKind = Kind.LIVE, sourceLabel = name)
                        } else {
                            emptyList()
                        }
                    }
                }
            } ?: emptyList()

            fun processAndAdd(entry: MediaEntry) {
                val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, entry.quality.ifBlank { "1080p" })
                val clean = M3uParser.cleanChannelTitle(entry.title)
                if (clean.isNotBlank()) {
                    allEntries.add(entry.copy(title = clean, type = Kind.LIVE, isVod = false, quality = qual))
                }
            }

            countryTasks.forEach { task ->
                task.await().forEach { processAndAdd(it) }
            }
            sportsTask.await().forEach { processAndAdd(it) }
            newsTask.await().forEach { processAndAdd(it) }
            documentaryTask.await().forEach { processAndAdd(it) }
            musicTask.await().forEach { processAndAdd(it) }
            comedyTask.await().forEach { processAndAdd(it) }
            autoTask.await().forEach { processAndAdd(it) }
            freeTvTask.await().forEach { processAndAdd(it) }
            customTasks.forEach { task ->
                task.await().forEach { processAndAdd(it) }
            }
        }

        val distinctList = allEntries.toList()
            .filter { it.url.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.url }
        cachedLiveChannels = distinctList
        if (context != null && distinctList.isNotEmpty()) {
            saveToDiskCache(context, "live", distinctList)
        }
        distinctList
    }

    /**
     * Load dedicated Pluto TV and FAST channels (Pluto All, Samsung TV+, Xiaomi, Rakuten TV UK, Plex etc.)
     */
    suspend fun loadPluto(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedPlutoChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedPlutoChannels
        }

        if (!forceRefresh && context != null) {
            val diskItems = loadFromDiskCache(context, "pluto")
            if (diskItems.isNotEmpty()) {
                cachedPlutoChannels = diskItems
                return@withContext diskItems
            }
        }

        val allEntries = Collections.synchronizedList(mutableListOf<MediaEntry>())

        coroutineScope {
            val plutoAllTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, PLUTO_ALL_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Pluto", defaultKind = Kind.PLUTO, sourceLabel = "Pluto TV")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Pluto | ${entry.group.trim().uppercase()}" else "Pluto | GENERAL"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "720p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val samsungAllTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, SAMSUNG_ALL_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Samsung", defaultKind = Kind.PLUTO, sourceLabel = "Samsung TV+")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Samsung | ${entry.group.trim().uppercase()}" else "Samsung | GENERAL"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val xiaomiTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, XIAOMI_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Xiaomi", defaultKind = Kind.PLUTO, sourceLabel = "Xiaomi TV")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Xiaomi | ${entry.group.trim().uppercase()}" else "Xiaomi | FAST"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val rakutenTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, RAKUTEN_UK_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "UK", defaultKind = Kind.PLUTO, sourceLabel = "Rakuten TV")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Rakuten | ${entry.group.trim().uppercase()}" else "Rakuten | UK"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, country = "UK", group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val plexAllTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, PLEX_ALL_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Plex", defaultKind = Kind.PLUTO, sourceLabel = "Plex FAST")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Plex | ${entry.group.trim().uppercase()}" else "Plex | FAST"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val plutoGbTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, PLUTO_GB_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "UK", defaultKind = Kind.PLUTO, sourceLabel = "Pluto TV UK")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Pluto | ${entry.group.trim().uppercase()}" else "Pluto | GENERAL"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "720p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, country = "UK", group = grp, quality = qual)
                    }
                } else emptyList()
            }

            val plutoUsTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, PLUTO_US_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "USA", defaultKind = Kind.PLUTO, sourceLabel = "Pluto TV USA")
                    parsed.map { entry ->
                        val grp = if (entry.group.isNotBlank()) "Pluto | ${entry.group.trim().uppercase()}" else "Pluto | GENERAL"
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "720p")
                        entry.copy(title = M3uParser.cleanChannelTitle(entry.title), type = Kind.PLUTO, country = "USA", group = grp, quality = qual)
                    }
                } else emptyList()
            }

            allEntries.addAll(plutoAllTask.await())
            allEntries.addAll(samsungAllTask.await())
            allEntries.addAll(xiaomiTask.await())
            allEntries.addAll(rakutenTask.await())
            allEntries.addAll(plexAllTask.await())
            allEntries.addAll(plutoGbTask.await())
            allEntries.addAll(plutoUsTask.await())
        }

        val distinctList = allEntries.toList().distinctBy { it.url }
        cachedPlutoChannels = distinctList
        if (context != null && distinctList.isNotEmpty()) {
            saveToDiskCache(context, "pluto", distinctList)
        }
        distinctList
    }

    /**
     * Load dedicated Sky & British Premier News & Entertainment Network
     */
    suspend fun loadSky(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedSkyChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedSkyChannels
        }

        if (!forceRefresh && context != null) {
            val diskItems = loadFromDiskCache(context, "sky")
            if (diskItems.isNotEmpty()) {
                cachedSkyChannels = diskItems
                return@withContext diskItems
            }
        }

        val skyList = curatedSkyChannels.map { it.copy(type = Kind.SKY, quality = it.quality.ifBlank { "1080p" }) }
        cachedSkyChannels = skyList
        if (context != null && skyList.isNotEmpty()) {
            saveToDiskCache(context, "sky", skyList)
        }
        skyList
    }

    /**
     * Load dynamic real Movies VOD catalog with feature film releases, poster art, and metadata.
     */
    suspend fun loadMovies(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedMovieChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedMovieChannels
        }

        if (!forceRefresh && context != null) {
            val diskItems = loadFromDiskCache(context, "movies")
            if (diskItems.isNotEmpty()) {
                cachedMovieChannels = diskItems
                return@withContext diskItems
            }
        }

        val allEntries = Collections.synchronizedList(mutableListOf<MediaEntry>())
        allEntries.addAll(curatedMovies)

        coroutineScope {
            val movieChannelsTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, MOVIES_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.MOVIE, sourceLabel = "Cinema")
                    parsed.map { entry ->
                        val clean = M3uParser.cleanChannelTitle(entry.title)
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(
                            title = clean,
                            type = Kind.MOVIE,
                            isVod = true,
                            quality = qual,
                            genre = if (entry.genre.isNotBlank()) entry.genre else (if (entry.group.isNotBlank() && !entry.group.equals("General", ignoreCase = true)) entry.group else "Feature Film"),
                            description = if (entry.description.isNotBlank()) entry.description else "Stream this feature release in high quality directly on Merlin TV."
                        )
                    }
                } else emptyList()
            }

            val classicMoviesTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, CLASSIC_MOVIES_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.MOVIE, sourceLabel = "Classic Cinema")
                    parsed.map { entry ->
                        val clean = M3uParser.cleanChannelTitle(entry.title)
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(
                            title = clean,
                            type = Kind.MOVIE,
                            isVod = true,
                            quality = qual,
                            genre = "Classic",
                            description = if (entry.description.isNotBlank()) entry.description else "Classic golden-age feature film streamed in high fidelity."
                        )
                    }
                } else emptyList()
            }

            allEntries.addAll(movieChannelsTask.await())
            allEntries.addAll(classicMoviesTask.await())
        }

        val distinctList = allEntries.toList().distinctBy { it.url }
        cachedMovieChannels = distinctList
        if (context != null && distinctList.isNotEmpty()) {
            saveToDiskCache(context, "movies", distinctList)
        }
        distinctList
    }

    /**
     * Load dynamic real TV Series VOD catalog with episodic shows and curated series.
     */
    suspend fun loadSeries(context: Context? = null, forceRefresh: Boolean = false): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (cachedSeriesChannels.isNotEmpty() && !forceRefresh) {
            return@withContext cachedSeriesChannels
        }

        if (!forceRefresh && context != null) {
            val diskItems = loadFromDiskCache(context, "series")
            if (diskItems.isNotEmpty()) {
                cachedSeriesChannels = diskItems
                return@withContext diskItems
            }
        }

        val allEntries = Collections.synchronizedList(mutableListOf<MediaEntry>())
        allEntries.addAll(curatedSeries)

        coroutineScope {
            val seriesTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, SERIES_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.SERIES, sourceLabel = "Series TV")
                    parsed.map { entry ->
                        val clean = M3uParser.cleanChannelTitle(entry.title)
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(
                            title = clean,
                            type = Kind.SERIES,
                            isVod = true,
                            quality = qual,
                            genre = if (entry.genre.isNotBlank()) entry.genre else "Drama",
                            description = if (entry.description.isNotBlank()) entry.description else "Episodic television broadcast on demand."
                        )
                    }
                } else emptyList()
            }

            val animationTask = async(Dispatchers.IO) {
                val body = fetchM3uContent(context, ANIMATION_SERIES_PLAYLIST)
                if (body.isNotBlank()) {
                    val parsed = M3uParser.parse(body, defaultCountry = "Global", defaultKind = Kind.SERIES, sourceLabel = "Animation TV")
                    parsed.map { entry ->
                        val clean = M3uParser.cleanChannelTitle(entry.title)
                        val qual = if (entry.quality.isNotBlank() && entry.quality != "1080p") entry.quality else detectQuality(entry.title, "1080p")
                        entry.copy(
                            title = clean,
                            type = Kind.SERIES,
                            isVod = true,
                            quality = qual,
                            genre = "Animation",
                            description = if (entry.description.isNotBlank()) entry.description else "Animated series broadcast on demand."
                        )
                    }
                } else emptyList()
            }

            allEntries.addAll(seriesTask.await())
            allEntries.addAll(animationTask.await())
        }

        val distinctList = allEntries.toList().distinctBy { it.url }
        cachedSeriesChannels = distinctList
        if (context != null && distinctList.isNotEmpty()) {
            saveToDiskCache(context, "series", distinctList)
        }
        distinctList
    }

    /**
     * Instantly returns curated and locally cached channels with 0ms network latency.
     */
    fun getInstantCatalogs(context: Context?): AllCatalogsResult {
        val cachedLive = if (cachedLiveChannels.isNotEmpty()) cachedLiveChannels else (if (context != null) loadFromDiskCache(context, "live", ignoreTtl = true) else emptyList()).ifEmpty { curatedLiveChannels }
        val cachedPluto = if (cachedPlutoChannels.isNotEmpty()) cachedPlutoChannels else (if (context != null) loadFromDiskCache(context, "pluto", ignoreTtl = true) else emptyList())
        val cachedSky = if (cachedSkyChannels.isNotEmpty()) cachedSkyChannels else (if (context != null) loadFromDiskCache(context, "sky", ignoreTtl = true) else emptyList()).ifEmpty { curatedSkyChannels }
        val cachedMovies = if (cachedMovieChannels.isNotEmpty()) cachedMovieChannels else (if (context != null) loadFromDiskCache(context, "movies", ignoreTtl = true) else emptyList()).ifEmpty { curatedMovies }
        val cachedSeries = if (cachedSeriesChannels.isNotEmpty()) cachedSeriesChannels else (if (context != null) loadFromDiskCache(context, "series", ignoreTtl = true) else emptyList()).ifEmpty { curatedSeries }
        return AllCatalogsResult(
            live = cachedLive,
            pluto = cachedPluto,
            sky = cachedSky,
            movies = cachedMovies,
            series = cachedSeries
        )
    }

    /**
     * Parallel loader for all catalogs simultaneously with full fault tolerance.
     */
    suspend fun loadAllCatalogs(context: Context? = null, forceRefresh: Boolean = false): AllCatalogsResult = withContext(boundedIO) {
        runCatching {
            coroutineScope {
                val liveDeferred = async(boundedIO) { runCatching { loadLive(context, forceRefresh) }.getOrDefault(cachedLiveChannels.ifEmpty { curatedLiveChannels }) }
                val plutoDeferred = async(boundedIO) { runCatching { loadPluto(context, forceRefresh) }.getOrDefault(cachedPlutoChannels) }
                val skyDeferred = async(boundedIO) { runCatching { loadSky(context, forceRefresh) }.getOrDefault(cachedSkyChannels.ifEmpty { curatedSkyChannels }) }
                val moviesDeferred = async(boundedIO) { runCatching { loadMovies(context, forceRefresh) }.getOrDefault(cachedMovieChannels.ifEmpty { curatedMovies }) }
                val seriesDeferred = async(boundedIO) { runCatching { loadSeries(context, forceRefresh) }.getOrDefault(cachedSeriesChannels.ifEmpty { curatedSeries }) }

                AllCatalogsResult(
                    live = liveDeferred.await(),
                    pluto = plutoDeferred.await(),
                    sky = skyDeferred.await(),
                    movies = moviesDeferred.await(),
                    series = seriesDeferred.await()
                )
            }
        }.getOrElse { error ->
            Timber.e(error, "Failed to load all catalogs, falling back to instant catalogs")
            getInstantCatalogs(context)
        }
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
        cachedLiveChannels = emptyList()
    }

    fun removeCustomPlaylist(context: Context, url: String) {
        val current = getCustomPlaylists(context).filterNot { it.second == url }
        val encoded = current.joinToString(";") { "${it.first}|${it.second}" }
        context.getSharedPreferences("merlin_playlists", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_urls", encoded)
            .apply()
        cachedLiveChannels = emptyList()
    }

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
            source = "BBC Broadcast",
            tvgId = "BBCNews.uk"
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
            source = "Sky UK",
            tvgId = "SkyNews.uk"
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
            source = "Bloomberg",
            tvgId = "BloombergTV.us"
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
            source = "Euronews",
            tvgId = "Euronews.fr"
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
            source = "France Medias Monde",
            tvgId = "France24English.fr"
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
            source = "Deutsche Welle",
            tvgId = "DWEnglish.de"
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
            source = "Red Bull Media House",
            tvgId = "RedBullTV.at"
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
            source = "NASA",
            tvgId = "NASATVMedia.us"
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
            source = "Thomson Reuters",
            tvgId = "ReutersNow.us"
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
            source = "ABC News",
            tvgId = "ABCNewsLive.us"
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
            source = "Paramount Global",
            tvgId = "CBSNews.us"
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
            source = "FOX Television Stations",
            tvgId = "LiveNOWfromFOX.us"
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
            source = "WeatherNation",
            tvgId = "WeatherNation.us"
        )
    )

    val curatedSkyChannels: List<MediaEntry> = listOf(
        MediaEntry(
            id = "sky-news-uk-fhd",
            title = "Sky News UK (1080p FHD)",
            url = "https://skynews-live.akamaized.net/hls/live/2026859/skynews_hd/master.m3u8",
            type = Kind.SKY,
            group = "Sky | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/9/90/Sky_News_logo_2020.svg/320px-Sky_News_logo_2020.svg.png",
            description = "Live breaking news, UK national stories, political reports, and global headlines.",
            source = "Sky Network",
            tvgId = "SkyNews.uk"
        ),
        MediaEntry(
            id = "sky-bbc-news-fhd",
            title = "BBC News UK (1080p FHD)",
            url = "https://vs-hls-push-uk-live.akamaized.net/x=4/i=urn:bbc:pips:service:bbc_news_channel_hd/t=3840/v=pv14/b=5070016/main.m3u8",
            type = Kind.SKY,
            group = "Sky | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/BBC_News_2022.svg/320px-BBC_News_2022.svg.png",
            description = "24-hour BBC News broadcast live in Full HD.",
            source = "BBC Network",
            tvgId = "BBCNews.uk"
        ),
        MediaEntry(
            id = "sky-bloomberg-europe",
            title = "Bloomberg Europe (1080p FHD)",
            url = "https://liveproduseast.global.ssl.fastly.net/us/Channel-HD-AWS-virginia-1/live.m3u8",
            type = Kind.SKY,
            group = "Sky | BUSINESS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Bloomberg_Television_logo.svg/320px-Bloomberg_Television_logo.svg.png",
            description = "European markets, financial analytics, stock trends, and economic reports.",
            source = "Bloomberg Network",
            tvgId = "BloombergTV.us"
        ),
        MediaEntry(
            id = "sky-euronews-world",
            title = "Euronews English (1080p FHD)",
            url = "https://euronews-euronews-world-1-gb.samsung.wurl.tv/playlist.m3u8",
            type = Kind.SKY,
            group = "Sky | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Euronews_2016_logo.svg/320px-Euronews_2016_logo.svg.png",
            description = "International news and European stories delivered 24/7.",
            source = "Euronews",
            tvgId = "Euronews.fr"
        ),
        MediaEntry(
            id = "sky-france24-english",
            title = "France 24 English (1080p FHD)",
            url = "https://f24hls-i.akamaihd.net/hls/live/221193/F24_EN_LO_HLS/master_2000.m3u8",
            type = Kind.SKY,
            group = "Sky | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/France_24_logo.svg/320px-France_24_logo.svg.png",
            description = "International perspectives and breaking world headlines.",
            source = "France 24",
            tvgId = "France24English.fr"
        ),
        MediaEntry(
            id = "sky-dw-english",
            title = "DW English Live (1080p FHD)",
            url = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            type = Kind.SKY,
            group = "Sky | DOCUMENTARY",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_logo.svg/320px-Deutsche_Welle_logo.svg.png",
            description = "Deutsche Welle documentary features, science, and world analysis.",
            source = "Deutsche Welle",
            tvgId = "DWEnglish.de"
        ),
        MediaEntry(
            id = "sky-reuters-tv",
            title = "Reuters TV Live (1080p FHD)",
            url = "https://reuters-reutersnow-1-us.rakuten.wurl.tv/playlist.m3u8",
            type = Kind.SKY,
            group = "Sky | NEWS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/Reuters_Logo.svg/320px-Reuters_Logo.svg.png",
            description = "Direct news feeds from journalists around the globe.",
            source = "Thomson Reuters",
            tvgId = "ReutersNow.us"
        ),
        MediaEntry(
            id = "sky-redbull-tv",
            title = "Red Bull TV Live (1080p FHD)",
            url = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            type = Kind.SKY,
            group = "Sky | SPORTS",
            country = "UK",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png",
            description = "Extreme sports, world championships, motorsports, and music festivals.",
            source = "Red Bull",
            tvgId = "RedBullTV.at"
        )
    )

    val curatedMovies: List<MediaEntry> = listOf(
        MediaEntry(
            id = "vod-movie-tears-of-steel",
            title = "Tears of Steel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            type = Kind.MOVIE,
            group = "Sci-Fi",
            genre = "Sci-Fi, Action",
            year = "2024",
            duration = "1h 48m",
            rating = "8.4 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Tears_of_Steel_poster.jpg/400px-Tears_of_Steel_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            description = "In a dystopian future, a group of warriors and scientists in Amsterdam attempt to save the earth from invading cyborg forces using a time-altering machine.",
            source = "Merlin Cinema",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-big-buck-bunny",
            title = "Big Buck Bunny",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            type = Kind.MOVIE,
            group = "Animation",
            genre = "Animation, Comedy, Family",
            year = "2023",
            duration = "1h 32m",
            rating = "8.6 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/400px-Big_buck_bunny_poster_big.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "A large, gentle rabbit seeks hilarious and inventive vengeance on three mischievous forest bullies who picked on his innocent friends.",
            source = "Merlin Cinema",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-sintel",
            title = "Sintel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            type = Kind.MOVIE,
            group = "Fantasy",
            genre = "Fantasy, Adventure, Action",
            year = "2024",
            duration = "1h 42m",
            rating = "8.5 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Sintel_poster.jpg/400px-Sintel_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            description = "A fierce young warrior searches the treacherous snowy peaks and mystical lands for a baby dragon she raised after it is abducted by an elder beast.",
            source = "Merlin Cinema",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-night-living-dead",
            title = "Night of the Living Dead",
            url = "https://ia800300.us.archive.org/29/items/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Horror, Thriller",
            year = "1968",
            duration = "1h 36m",
            rating = "8.9 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Night_of_the_Living_Dead_%281968%29_poster.jpg/400px-Night_of_the_Living_Dead_%281968%29_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "George A. Romero's iconic horror masterpiece. A disparate group of strangers barricade themselves in an isolated farmhouse as the dead rise from their graves.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-charade",
            title = "Charade",
            url = "https://ia800301.us.archive.org/17/items/Charade1963/Charade_512kb.mp4",
            type = Kind.MOVIE,
            group = "Thriller",
            genre = "Mystery, Romance, Thriller",
            year = "1963",
            duration = "1h 53m",
            rating = "8.8 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Charade_poster.jpg/400px-Charade_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
            description = "Starring Audrey Hepburn and Cary Grant. A woman is pursued across Paris by several dangerous men searching for a fortune stolen by her late husband.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-his-girl-friday",
            title = "His Girl Friday",
            url = "https://ia800201.us.archive.org/22/items/his_girl_friday/his_girl_friday_512kb.mp4",
            type = Kind.MOVIE,
            group = "Comedy",
            genre = "Comedy, Romance, Classic",
            year = "1940",
            duration = "1h 32m",
            rating = "8.5 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/His_Girl_Friday_poster.jpg/400px-His_Girl_Friday_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80",
            description = "A rapid-fire screwball comedy starring Cary Grant and Rosalind Russell. A newspaper editor uses every wild trick to keep his ace reporter ex-wife from leaving.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-the-general",
            title = "The General",
            url = "https://ia800200.us.archive.org/31/items/TheGeneral1926/TheGeneral_512kb.mp4",
            type = Kind.MOVIE,
            group = "Action",
            genre = "Action, Comedy, Classic",
            year = "1926",
            duration = "1h 18m",
            rating = "8.8 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/The_General_%281926%29_poster.jpg/400px-The_General_%281926%29_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
            description = "Buster Keaton's legendary action masterpiece. A courageous locomotive engineer single-handedly races across enemy territory to reclaim his stolen train.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-nosferatu",
            title = "Nosferatu: A Symphony of Horror",
            url = "https://ia800300.us.archive.org/33/items/Nosferatu_1922/Nosferatu_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Horror, Mystery, Gothic",
            year = "1922",
            duration = "1h 34m",
            rating = "8.7 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/NosferatuPoster.jpg/400px-NosferatuPoster.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "The immortal silent vampire classic directed by F.W. Murnau. The mysterious Count Orlok brings darkness, pestilence, and fear as he arrives in Wisborg.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-house-haunted-hill",
            title = "House on Haunted Hill",
            url = "https://ia800302.us.archive.org/26/items/House_On_Haunted_Hill_1959/House_on_Haunted_Hill_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Horror, Mystery, Thriller",
            year = "1959",
            duration = "1h 15m",
            rating = "8.2 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/House_on_Haunted_Hill_poster.jpg/400px-House_on_Haunted_Hill_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "Vincent Price stars as an eccentric millionaire who invites five guests to a haunted mansion, offering ten thousand dollars to anyone who survives until sunrise.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-carnival-of-souls",
            title = "Carnival of Souls",
            url = "https://ia800303.us.archive.org/1/items/Carnival_of_Souls/Carnival_of_Souls_512kb.mp4",
            type = Kind.MOVIE,
            group = "Thriller",
            genre = "Psychological Thriller, Horror",
            year = "1962",
            duration = "1h 18m",
            rating = "8.1 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Carnival_of_Souls_poster.jpg/400px-Carnival_of_Souls_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
            description = "After surviving a tragic car accident, a church organist is drawn towards an abandoned lakeside pavilion where ghostly apparitions begin to stalk her.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-little-shop-horrors",
            title = "The Little Shop of Horrors",
            url = "https://ia800303.us.archive.org/34/items/Little_Shop_of_Horrors/Little_Shop_of_Horrors_512kb.mp4",
            type = Kind.MOVIE,
            group = "Comedy",
            genre = "Comedy, Horror, Cult",
            year = "1960",
            duration = "1h 12m",
            rating = "7.9 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Little_shop_of_horrors_poster.jpg/400px-Little_shop_of_horrors_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80",
            description = "A clumsy young florist creates a strange new carnivorous plant that develops an insatiable appetite for human blood, causing chaotic comedy.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-dressed-to-kill",
            title = "Sherlock Holmes: Dressed to Kill",
            url = "https://ia800303.us.archive.org/16/items/DressedToKill_683/DressedToKill_512kb.mp4",
            type = Kind.MOVIE,
            group = "Thriller",
            genre = "Mystery, Crime, Detective",
            year = "1946",
            duration = "1h 12m",
            rating = "8.3 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Dressed_to_kill_poster.jpg/400px-Dressed_to_kill_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
            description = "Basil Rathbone stars as the great detective Sherlock Holmes as he races against a ruthless criminal gang to decipher secret musical code boxes.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-night-living-dead-2",
            title = "Night of the Living Dead",
            url = "https://ia800301.us.archive.org/16/items/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Horror, Cult, Sci-Fi",
            year = "1968",
            duration = "1h 36m",
            rating = "8.9 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Night_of_the_Living_Dead_%281968%29_poster.jpg/400px-Night_of_the_Living_Dead_%281968%29_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "George A. Romero's iconic horror masterpiece. A disparate group of individuals seeks refuge in an abandoned farmhouse while defending against undead ghouls.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-plan-9",
            title = "Plan 9 from Outer Space",
            url = "https://ia800300.us.archive.org/3/items/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4",
            type = Kind.MOVIE,
            group = "Sci-Fi",
            genre = "Sci-Fi, Cult, Mystery",
            year = "1957",
            duration = "1h 19m",
            rating = "7.8 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Plan_9_from_Outer_Space_poster.jpg/400px-Plan_9_from_Outer_Space_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            description = "Ed Wood's legendary cult sci-fi film featuring Bela Lugosi. Extraterrestrials enact Plan 9 to resurrect the dead and prevent humanity from building a doomsday bomb.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-phantom-opera",
            title = "The Phantom of the Opera",
            url = "https://ia800303.us.archive.org/2/items/ThePhantomoftheOpera1925_466/PhantomOfTheOpera1925_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Horror, Drama, Gothic",
            year = "1925",
            duration = "1h 33m",
            rating = "8.6 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Phantom_of_the_Opera_%281925_poster%29.jpg/400px-Phantom_of_the_Opera_%281925_poster%29.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "Lon Chaney stars as the disfigured Phantom who haunts the Paris Opera House, manipulating performances to make the woman he loves a star.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-gullivers-travels",
            title = "Gulliver's Travels",
            url = "https://ia800201.us.archive.org/13/items/gullivers_travels1939/gullivers_travels1939_512kb.mp4",
            type = Kind.MOVIE,
            group = "Animation",
            genre = "Animation, Fantasy, Family",
            year = "1939",
            duration = "1h 16m",
            rating = "8.3 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Gulliver%27s_Travels_%281939_poster%29.jpg/400px-Gulliver%27s_Travels_%281939_poster%29.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "Max Fleischer's classic animated feature. Shipwrecked Lemuel Gulliver washes ashore on the island kingdom of Lilliput and helps resolve a royal conflict.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-gold-rush",
            title = "The Gold Rush",
            url = "https://ia800301.us.archive.org/20/items/TheGoldRush1925/TheGoldRush_512kb.mp4",
            type = Kind.MOVIE,
            group = "Comedy",
            genre = "Comedy, Adventure, Classic",
            year = "1925",
            duration = "1h 35m",
            rating = "8.9 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/06/The_Gold_Rush_poster.jpg/400px-The_Gold_Rush_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80",
            description = "Charlie Chaplin's timeless comedy masterpiece. The Little Tramp travels to the snowy Yukon in search of gold and finds love, adventure, and danger.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-tears-of-steel-2",
            title = "Tears of Steel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            type = Kind.MOVIE,
            group = "Sci-Fi",
            genre = "Sci-Fi, Action, VFX",
            year = "2012",
            duration = "12m",
            rating = "8.4 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Tears_of_Steel_poster.jpg/400px-Tears_of_Steel_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            description = "Open source sci-fi cinema showcasing high-end visual effects. A group of scientists and warriors in dystopian Amsterdam work to save the future of humanity.",
            source = "Open Cinema Studio",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-big-buck-bunny-2",
            title = "Big Buck Bunny (1080p FHD)",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            type = Kind.MOVIE,
            group = "Animation",
            genre = "Animation, Comedy, Family",
            year = "2008",
            duration = "10m",
            rating = "8.5 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/400px-Big_buck_bunny_poster_big.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "A giant, gentle bunny decides to take revenge on three forest bullies who harass innocent butterflies and woodland creatures.",
            source = "Open Cinema Studio",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-sintel-2",
            title = "Sintel: The Dragon Quest",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            type = Kind.MOVIE,
            group = "Animation",
            genre = "Animation, Fantasy, Adventure",
            year = "2010",
            duration = "15m",
            rating = "8.8 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Sintel_poster.jpg/400px-Sintel_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "A lonely young woman named Sintel embarks on an epic and emotional journey across dangerous mountains and deserts to rescue her pet baby dragon.",
            source = "Open Cinema Studio",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-last-man-on-earth",
            title = "The Last Man on Earth",
            url = "https://ia800303.us.archive.org/7/items/TheLastManOnEarth_1964/TheLastManOnEarth_1964_512kb.mp4",
            type = Kind.MOVIE,
            group = "Horror",
            genre = "Sci-Fi, Horror, Post-Apocalyptic",
            year = "1964",
            duration = "1h 26m",
            rating = "8.3 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/The_Last_Man_on_Earth_%281964%29_poster.jpg/400px-The_Last_Man_on_Earth_%281964%29_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
            description = "Vincent Price stars as Dr. Robert Morgan, the sole human survivor of a global airborne plague that transformed the rest of humanity into nocturnal creatures.",
            source = "Classic Cinema Vault",
            isVod = true
        ),
        MediaEntry(
            id = "vod-movie-doa-1949",
            title = "D.O.A.",
            url = "https://ia800303.us.archive.org/29/items/doa_1949/doa_1949_512kb.mp4",
            type = Kind.MOVIE,
            group = "Thriller",
            genre = "Film Noir, Crime, Mystery",
            year = "1949",
            duration = "1h 23m",
            rating = "8.5 ★",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/DOA_1950_poster.jpg/400px-DOA_1950_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
            description = "An accountant discovers he has been poisoned with a lethal, slow-acting toxin and has only hours to live as he frantically tracks down his murderer.",
            source = "Classic Cinema Vault",
            isVod = true
        )
    )

    val curatedSeries: List<MediaEntry> = listOf(
        MediaEntry(
            id = "vod-series-beverly-hillbillies-s1e1",
            title = "The Beverly Hillbillies",
            url = "https://ia800300.us.archive.org/1/items/Beverly_Hillbillies_1/Beverly_Hillbillies_1_512kb.mp4",
            type = Kind.SERIES,
            group = "Comedy",
            genre = "Classic Sitcom, Comedy",
            year = "1962",
            duration = "26m",
            rating = "8.5 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Beverly_Hillbillies_cast.jpg/400px-Beverly_Hillbillies_cast.jpg",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80",
            description = "S1:E1 - 'The Clampetts Strike Oil'. A poor Ozark mountaineer discovers crude oil on his land and moves his colorful eccentric family into a luxury Beverly Hills mansion.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-beverly-hillbillies-s1e2",
            title = "The Beverly Hillbillies",
            url = "https://ia800300.us.archive.org/1/items/Beverly_Hillbillies_2/Beverly_Hillbillies_2_512kb.mp4",
            type = Kind.SERIES,
            group = "Comedy",
            genre = "Classic Sitcom, Comedy",
            year = "1962",
            duration = "25m",
            rating = "8.4 ★",
            season = 1,
            episode = 2,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Beverly_Hillbillies_cast.jpg/400px-Beverly_Hillbillies_cast.jpg",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800&q=80",
            description = "S1:E2 - 'Getting Settled'. Jed and the clan try to adapt to their lavish 36-room mansion with its mysterious modern contraptions and swimming pool.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-bonanza-s1e1",
            title = "Bonanza",
            url = "https://ia800300.us.archive.org/31/items/Bonanza_Episode_1/Bonanza_Episode_1_512kb.mp4",
            type = Kind.SERIES,
            group = "Drama",
            genre = "Western, Action, Drama",
            year = "1959",
            duration = "49m",
            rating = "8.7 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bonanza_Cast_1960.JPG/400px-Bonanza_Cast_1960.JPG",
            backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
            description = "S1:E1 - 'A Rose for Lotta'. The legendary Cartwright clan battles to defend their 600,000-acre Ponderosa ranch in Nevada from ruthless timber mining barons.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-bonanza-s1e2",
            title = "Bonanza",
            url = "https://ia800300.us.archive.org/31/items/Bonanza_Episode_2/Bonanza_Episode_2_512kb.mp4",
            type = Kind.SERIES,
            group = "Drama",
            genre = "Western, Action, Drama",
            year = "1959",
            duration = "48m",
            rating = "8.6 ★",
            season = 1,
            episode = 2,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Bonanza_Cast_1960.JPG/400px-Bonanza_Cast_1960.JPG",
            backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
            description = "S1:E2 - 'Death on Sun Mountain'. Ben Cartwright confronts a ruthless band of claim jumpers who threaten the peace and stability of Virginia City.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-superman-s1e1",
            title = "Superman Animated: The Mad Scientist",
            url = "https://ia800201.us.archive.org/11/items/superman_1941/superman_1941_512kb.mp4",
            type = Kind.SERIES,
            group = "Animation",
            genre = "Animation, Superhero, Action",
            year = "1941",
            duration = "11m",
            rating = "8.9 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Superman_1941_title.png/400px-Superman_1941_title.png",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "S1:E1 - Fleischer Studios groundbreaking animation. Superman defends Metropolis when a villainous scientist fires a devastating energy death-ray from a mountain fortress.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-superman-s1e2",
            title = "Superman Animated: Mechanical Monsters",
            url = "https://ia800300.us.archive.org/28/items/mechanical_monsters/mechanical_monsters_512kb.mp4",
            type = Kind.SERIES,
            group = "Animation",
            genre = "Animation, Superhero, Action",
            year = "1941",
            duration = "11m",
            rating = "8.8 ★",
            season = 1,
            episode = 2,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Mechanical_monsters.jpg/400px-Mechanical_monsters.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "S1:E2 - Superman takes flight against a fleet of flying robotic mechanical monsters robbing precious jewels and gold from the city vaults.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-superman-s1e3",
            title = "Superman Animated: Billion Dollar Limited",
            url = "https://ia800300.us.archive.org/19/items/billion_dollar_limited/billion_dollar_limited_512kb.mp4",
            type = Kind.SERIES,
            group = "Animation",
            genre = "Animation, Superhero, Action",
            year = "1942",
            duration = "10m",
            rating = "8.7 ★",
            season = 1,
            episode = 3,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Superman_1941_title.png/400px-Superman_1941_title.png",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "S1:E3 - A heavily armored train carrying one billion dollars in gold bullion is hijacked by armed bandits, leaving Superman to stop the runaway train.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-popeye-s1e1",
            title = "Popeye: Meets Sinbad the Sailor",
            url = "https://ia800300.us.archive.org/27/items/popeye_meets_sinbad_the_sailor/popeye_meets_sinbad_the_sailor_512kb.mp4",
            type = Kind.SERIES,
            group = "Animation",
            genre = "Animation, Comedy, Adventure",
            year = "1936",
            duration = "16m",
            rating = "8.7 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Popeye_the_Sailor_Meets_Sindbad_the_Sailor_poster.jpg/400px-Popeye_the_Sailor_Meets_Sindbad_the_Sailor_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "S1:E1 - Popeye, Olive Oyl, and J. Wellington Wimpy sail to the legendary island of Sinbad where Popeye battles mythical dragons and giant birds.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-popeye-s1e2",
            title = "Popeye: Meets Ali Baba and the 40 Thieves",
            url = "https://ia800300.us.archive.org/20/items/popeye_meets_ali_baba_and_his_forty_thieves/popeye_meets_ali_baba_and_his_forty_thieves_512kb.mp4",
            type = Kind.SERIES,
            group = "Animation",
            genre = "Animation, Comedy, Adventure",
            year = "1937",
            duration = "17m",
            rating = "8.6 ★",
            season = 1,
            episode = 2,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Popeye_the_Sailor_Meets_Ali_Baba%27s_Forty_Thieves_poster.jpg/400px-Popeye_the_Sailor_Meets_Ali_Baba%27s_Forty_Thieves_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
            description = "S1:E2 - Stationed at a lonely Coast Guard outpost, Popeye and Olive defend the desert fortress from the notorious outlaw Abu Hassan and his army.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-dragnet-s1e1",
            title = "Dragnet",
            url = "https://ia800300.us.archive.org/26/items/Dragnet_The_Big_Cast/Dragnet_The_Big_Cast_512kb.mp4",
            type = Kind.SERIES,
            group = "Drama",
            genre = "Crime, Mystery, Police Procedural",
            year = "1952",
            duration = "26m",
            rating = "8.4 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/71/Dragnet_1952.JPG/400px-Dragnet_1952.JPG",
            backdrop = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80",
            description = "S1:E1 - 'The Big Cast'. 'Ladies and gentlemen, the story you are about to hear is true.' Sergeant Joe Friday tracks down a cunning criminal across Los Angeles.",
            source = "Classic TV Series",
            isVod = true
        ),
        MediaEntry(
            id = "vod-series-flash-gordon-s1e1",
            title = "Flash Gordon Conquers the Universe",
            url = "https://ia800300.us.archive.org/30/items/Flash_Gordon_Conquers_the_Universe_Ch1/Flash_Gordon_Conquers_the_Universe_Ch1_512kb.mp4",
            type = Kind.SERIES,
            group = "Sci-Fi",
            genre = "Sci-Fi, Space Opera, Action",
            year = "1940",
            duration = "21m",
            rating = "8.6 ★",
            season = 1,
            episode = 1,
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Flash_Gordon_Conquers_the_Universe_poster.jpg/400px-Flash_Gordon_Conquers_the_Universe_poster.jpg",
            backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
            description = "S1:E1 - 'The Purple Death'. Buster Crabbe stars as Flash Gordon as he rockets into deep space to battle the evil tyrant Ming the Merciless.",
            source = "Classic TV Series",
            isVod = true
        )
    )
}