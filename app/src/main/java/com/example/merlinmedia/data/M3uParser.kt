package com.example.merlinmedia.data

import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry

object M3uParser {
    private val LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val GROUP_REGEX = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)

    fun parse(m3uContent: String, defaultCountry: String = "", sourceLabel: String = "IPTV Playlist"): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = ""
        var currentId = ""

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

                // Extract Title (after last comma)
                val commaIndex = line.lastIndexOf(',')
                currentTitle = if (commaIndex != -1 && commaIndex + 1 < line.length) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    TVG_NAME_REGEX.find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { "Channel" }
                }
            } else if (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) {
                if (currentTitle.isBlank()) {
                    currentTitle = "Channel ${entries.size + 1}"
                }
                val entryId = if (currentId.isNotBlank()) currentId else "$currentTitle-$line".hashCode().toString()
                
                entries.add(
                    MediaEntry(
                        id = entryId,
                        title = currentTitle,
                        url = line,
                        type = Kind.LIVE,
                        country = defaultCountry,
                        group = currentGroup.ifBlank { "General" },
                        logo = currentLogo,
                        source = sourceLabel
                    )
                )
                
                // Reset per-entry state
                currentTitle = ""
                currentLogo = null
                currentGroup = ""
                currentId = ""
            }
        }
        return entries
    }
}