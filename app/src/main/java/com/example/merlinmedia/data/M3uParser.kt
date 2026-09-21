package com.example.merlinmedia.data

import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry

object M3uParser {
    private val LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val GROUP_REGEX = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_COUNTRY_REGEX = Regex("""tvg-country="([^"]*)"""", RegexOption.IGNORE_CASE)

    // Markers for streams that are known dead, geo-restricted, or intermittent
    private val DEAD_OR_BLOCKED_MARKERS = listOf(
        "[geo-blocked]",
        "[geoblocked]",
        "[offline]",
        "[dead]",
        "[blocked]",
        "[offline/dead]",
        "[no stream]"
    )

    // Web domains that ExoPlayer cannot stream directly without browser/JS extractors
    private val NON_DIRECT_STREAM_DOMAINS = listOf(
        "youtube.com",
        "youtu.be",
        "twitch.tv",
        "dailymotion.com",
        "facebook.com",
        "vimeo.com",
        "ok.ru",
        "tiktok.com"
    )

    private val QUALITY_TAG_REGEX = Regex("""\s*[\(\[]\s*(\d{3,4}p|\d+fps|4k|uhd|fhd|hd|sd|hevc|h264|h265)\s*[\)\]]""", RegexOption.IGNORE_CASE)
    private val BRACKET_TAG_REGEX = Regex("""\s*\[[^\]]*\]""")

    fun normalizeCountry(raw: String): String {
        val trimmed = raw.trim().uppercase()
        return when (trimmed) {
            "UK", "GB", "GBR", "UNITED KINGDOM", "GREAT BRITAIN", "ENGLAND" -> "UK"
            "US", "USA", "UNITED STATES", "UNITED STATES OF AMERICA" -> "USA"
            "CA", "CAN", "CANADA" -> "Canada"
            "AU", "AUS", "AUSTRALIA" -> "Australia"
            "FR", "FRA", "FRANCE" -> "France"
            "DE", "DEU", "GER", "GERMANY" -> "Germany"
            "ES", "ESP", "SPAIN" -> "Spain"
            "IT", "ITA", "ITALY" -> "Italy"
            else -> raw.trim()
        }
    }

    fun cleanChannelTitle(raw: String): String {
        return raw
            .replace(QUALITY_TAG_REGEX, "")
            .replace(BRACKET_TAG_REGEX, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun parse(
        m3uContent: String,
        defaultCountry: String = "",
        defaultKind: Kind = Kind.LIVE,
        sourceLabel: String = "IPTV Playlist"
    ): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = ""
        var currentId = ""
        var currentCountry = defaultCountry

        for (rawLine in m3uContent.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                // Extract tvg-logo
                val logoMatch = LOGO_REGEX.find(line)?.groupValues?.getOrNull(1)
                currentLogo = if (!logoMatch.isNullOrBlank()) logoMatch else null

                // Extract group-title
                currentGroup = GROUP_REGEX.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty()

                // Extract tvg-id
                currentId = TVG_ID_REGEX.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty()

                // Extract tvg-country if available and normalize
                val countryMatch = TVG_COUNTRY_REGEX.find(line)?.groupValues?.getOrNull(1)?.trim()
                currentCountry = when {
                    defaultCountry.isNotBlank() -> defaultCountry
                    !countryMatch.isNullOrBlank() -> normalizeCountry(countryMatch)
                    else -> "Global"
                }

                // Extract Title (after last comma)
                val commaIndex = line.lastIndexOf(',')
                currentTitle = if (commaIndex != -1 && commaIndex + 1 < line.length) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    TVG_NAME_REGEX.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Channel" }
                }
            } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                if (currentGroup.isBlank()) {
                    currentGroup = line.substringAfter(":").trim()
                }
            } else if (
                line.startsWith("http://", ignoreCase = true) ||
                line.startsWith("https://", ignoreCase = true) ||
                line.startsWith("rtsp://", ignoreCase = true) ||
                line.startsWith("rtmp://", ignoreCase = true) ||
                line.startsWith("mms://", ignoreCase = true)
            ) {
                val lowerTitle = currentTitle.lowercase()
                val lowerLine = line.lowercase()

                // 1. Filter out known dead or geoblocked streams
                val isDeadOrBlocked = DEAD_OR_BLOCKED_MARKERS.any { marker ->
                    lowerTitle.contains(marker) || lowerLine.contains(marker)
                }

                // 2. Filter out non-stream web embeds
                val isNonDirectStream = NON_DIRECT_STREAM_DOMAINS.any { domain ->
                    lowerLine.contains(domain)
                }

                // 3. Filter out plain HTML or invalid pages
                val isHtmlPage = lowerLine.endsWith(".html") || lowerLine.endsWith(".htm")

                if (!isDeadOrBlocked && !isNonDirectStream && !isHtmlPage) {
                    val parsedQuality = when {
                        lowerTitle.contains("4k") || lowerTitle.contains("uhd") || lowerTitle.contains("2160p") -> "4K"
                        lowerTitle.contains("1080p") || lowerTitle.contains("1080") || lowerTitle.contains("fhd") -> "1080p"
                        lowerTitle.contains("720p") || lowerTitle.contains("720") || lowerTitle.contains("hd") -> "720p"
                        lowerTitle.contains("576p") || lowerTitle.contains("480p") || lowerTitle.contains("sd") -> "SD"
                        else -> "1080p"
                    }

                    val cleanTitle = cleanChannelTitle(currentTitle).ifBlank { "Channel ${entries.size + 1}" }
                    val entryId = if (currentId.isNotBlank()) currentId else "$cleanTitle-$line".hashCode().toString()

                    entries.add(
                        MediaEntry(
                            id = entryId,
                            title = cleanTitle,
                            url = line,
                            type = defaultKind,
                            country = currentCountry,
                            group = currentGroup.ifBlank { "General" },
                            logo = currentLogo,
                            source = sourceLabel,
                            quality = parsedQuality,
                            tvgId = currentId
                        )
                    )
                }

                // Reset per-entry state
                currentTitle = ""
                currentLogo = null
                currentGroup = ""
                currentId = ""
                currentCountry = defaultCountry
            }
        }
        return entries
    }
}