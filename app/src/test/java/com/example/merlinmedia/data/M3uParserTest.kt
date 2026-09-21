package com.example.merlinmedia.data

import com.example.merlinmedia.model.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun testParseSimpleM3u() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="BBC1.uk" tvg-name="BBC One HD" tvg-logo="https://example.com/logo.png" group-title="UK | ENTERTAINMENT",BBC One HD (1080p)
            https://example.com/stream.m3u8
        """.trimIndent()

        val list = M3uParser.parse(content, defaultCountry = "UK", defaultKind = Kind.LIVE)
        assertEquals(1, list.size)

        val entry = list[0]
        assertEquals("BBC One HD", entry.title)
        assertEquals("https://example.com/stream.m3u8", entry.url)
        assertEquals("BBC1.uk", entry.tvgId)
        assertEquals("UK | ENTERTAINMENT", entry.group)
        assertEquals("https://example.com/logo.png", entry.logo)
        assertEquals("1080p", entry.quality)
    }

    @Test
    fun testParenthesesRetentionForYearsAndMeta() {
        val cleaned = M3uParser.cleanChannelTitle("Doctor Who (2024) (1080p) [geo-blocked]")
        assertEquals("Doctor Who (2024)", cleaned)
    }

    @Test
    fun testFilterOutDeadAndWebStreams() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Dead Stream [dead]",Dead Stream [dead]
            https://example.com/dead.m3u8
            #EXTINF:-1 tvg-name="YouTube Embed",YouTube Embed
            https://www.youtube.com/watch?v=12345
            #EXTINF:-1 tvg-name="Valid Stream",Valid Stream
            https://example.com/valid.m3u8
        """.trimIndent()

        val list = M3uParser.parse(content)
        assertEquals(1, list.size)
        assertEquals("Valid Stream", list[0].title)
    }
}
