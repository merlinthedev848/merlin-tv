package com.example.merlinmedia.data

import android.content.Context
import com.example.merlinmedia.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EpgRepository {
    private const val PLEX_EPG_URL = "https://i.mjh.nz/Plex/all.xml"
    private const val CACHE_FILE_NAME = "epg_plex.xml"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    // In-memory index: channelId/channelName (lowercase) -> List of RawProgramme
    private data class RawProgramme(
        val channel: String,
        val startMillis: Long,
        val stopMillis: Long,
        val title: String,
        val desc: String
    )

    private val epgData = ConcurrentHashMap<String, MutableList<RawProgramme>>()
    @Volatile private var isInitialized = false
    @Volatile private var isLoading = false

    private val xmlDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val timeDisplayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized || isLoading) return@withContext
        isLoading = true
        try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            val now = System.currentTimeMillis()

            val xmlContent = if (cacheFile.exists() && (now - cacheFile.lastModified() < CACHE_TTL_MS)) {
                cacheFile.readText()
            } else {
                val client = HttpClientProvider.getClient(context)
                val request = Request.Builder()
                    .url(PLEX_EPG_URL)
                    .header("User-Agent", "MerlinTV/1.4.3")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        runCatching { cacheFile.writeText(body) }
                    }
                    body
                } else {
                    if (cacheFile.exists()) cacheFile.readText() else ""
                }
            }

            if (xmlContent.isNotBlank()) {
                parseXmlTv(xmlContent)
                isInitialized = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    private fun parseXmlTv(xml: String) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentChannel: String? = null
        var currentStart: Long = 0
        var currentStop: Long = 0
        var currentTitle = ""
        var currentDesc = ""
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("programme", ignoreCase = true)) {
                        currentChannel = parser.getAttributeValue(null, "channel")
                        val startStr = parser.getAttributeValue(null, "start")
                        val stopStr = parser.getAttributeValue(null, "stop")
                        currentStart = parseXmlDate(startStr)
                        currentStop = parseXmlDate(stopStr)
                        currentTitle = ""
                        currentDesc = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        if (currentTag.equals("title", ignoreCase = true)) {
                            currentTitle = text
                        } else if (currentTag.equals("desc", ignoreCase = true)) {
                            currentDesc = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("programme", ignoreCase = true) && currentChannel != null) {
                        val prog = RawProgramme(
                            channel = currentChannel,
                            startMillis = currentStart,
                            stopMillis = currentStop,
                            title = currentTitle,
                            desc = currentDesc
                        )
                        val key = currentChannel.lowercase()
                        epgData.computeIfAbsent(key) { Collections.synchronizedList(mutableListOf()) }.add(prog)
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseXmlDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return runCatching {
            xmlDateFormat.parse(dateStr)?.time ?: 0L
        }.getOrDefault(0L)
    }

    /**
     * Finds real Now and Next programmes for a channel by tvgId or fallback name.
     */
    fun getNowAndNext(tvgId: String?, channelTitle: String?): Pair<EpgProgram, EpgProgram?>? {
        val now = System.currentTimeMillis()
        val list = (tvgId?.lowercase()?.let { epgData[it] }
            ?: channelTitle?.lowercase()?.let { epgData[it] }) ?: return null

        val currentProgs = synchronized(list) {
            list.filter { it.stopMillis > now }
                .sortedBy { it.startMillis }
        }

        val active = currentProgs.firstOrNull { it.startMillis <= now && it.stopMillis > now }
            ?: currentProgs.firstOrNull() ?: return null

        val next = currentProgs.firstOrNull { it.startMillis >= active.stopMillis }

        val totalDuration = (active.stopMillis - active.startMillis).coerceAtLeast(1L)
        val elapsed = (now - active.startMillis).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0.05f, 0.95f)

        val nowProg = EpgProgram(
            title = active.title,
            startFormatted = timeDisplayFormat.format(Date(active.startMillis)),
            stopFormatted = timeDisplayFormat.format(Date(active.stopMillis)),
            description = active.desc,
            progress = progress
        )

        val nextProg = next?.let {
            EpgProgram(
                title = it.title,
                startFormatted = timeDisplayFormat.format(Date(it.startMillis)),
                stopFormatted = timeDisplayFormat.format(Date(it.stopMillis)),
                description = it.desc,
                progress = 0f
            )
        }

        return Pair(nowProg, nextProg)
    }
}
