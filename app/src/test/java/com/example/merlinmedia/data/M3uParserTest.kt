package com.example.merlinmedia.data

import com.example.merlinmedia.model.Kind
import org.junit.Assert.*
import org.junit.Test

class M3uParserTest {

    @Test
    fun `parse simple m3u with one entry`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="bbc1" tvg-logo="https://logo.com/bbc1.png" group-title="UK | News",BBC One (1080p)
            https://stream.example.com/bbc1.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content, defaultCountry = "UK", defaultKind = Kind.LIVE, sourceLabel = "Test")

        assertEquals(1, result.size)
        val entry = result[0]
        assertEquals("BBC One", entry.title) // Should have quality tag stripped
        assertEquals("https://stream.example.com/bbc1.m3u8", entry.url)
        assertEquals(Kind.LIVE, entry.type)
        assertEquals("UK", entry.country)
        assertEquals("UK | News", entry.group)
        assertEquals("https://logo.com/bbc1.png", entry.logo)
        assertEquals("1080p", entry.quality)
        assertEquals("bbc1", entry.tvgId)
    }

    @Test
    fun `parse filters out youtube urls`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,YouTube Stream
            https://www.youtube.com/watch?v=12345
            #EXTINF:-1,Valid Stream
            https://stream.example.com/live.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.size)
        assertEquals("Valid Stream", result[0].title)
    }

    @Test
    fun `parse filters out geo-blocked entries`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Blocked Channel [geo-blocked]
            https://stream.example.com/blocked.m3u8
            #EXTINF:-1,Good Channel
            https://stream.example.com/good.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.size)
        assertEquals("Good Channel", result[0].title)
    }

    @Test
    fun `parse handles empty content`() {
        val result = M3uParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse handles content with no valid entries`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Just metadata
        """.trimIndent()

        val result = M3uParser.parse(content)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse detects 4K quality from title`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Nature Documentary (4K UHD)
            https://stream.example.com/nature4k.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.size)
        assertEquals("4K", result[0].quality)
    }

    @Test
    fun `parse multiple entries preserves order`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Channel A
            https://stream.example.com/a.m3u8
            #EXTINF:-1,Channel B
            https://stream.example.com/b.m3u8
            #EXTINF:-1,Channel C
            https://stream.example.com/c.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(3, result.size)
        assertEquals("Channel A", result[0].title)
        assertEquals("Channel B", result[1].title)
        assertEquals("Channel C", result[2].title)
    }

    @Test
    fun `normalizeCountry handles common variants`() {
        assertEquals("UK", M3uParser.normalizeCountry("GB"))
        assertEquals("UK", M3uParser.normalizeCountry("United Kingdom"))
        assertEquals("USA", M3uParser.normalizeCountry("US"))
        assertEquals("USA", M3uParser.normalizeCountry("United States"))
        assertEquals("Canada", M3uParser.normalizeCountry("CA"))
        assertEquals("Australia", M3uParser.normalizeCountry("AU"))
        assertEquals("France", M3uParser.normalizeCountry("FR"))
        assertEquals("Germany", M3uParser.normalizeCountry("DE"))
    }

    @Test
    fun `cleanChannelTitle removes quality tags`() {
        assertEquals("BBC News", M3uParser.cleanChannelTitle("BBC News (1080p)"))
        assertEquals("Sky Sports", M3uParser.cleanChannelTitle("Sky Sports [4K]"))
        assertEquals("CNN", M3uParser.cleanChannelTitle("CNN [HD]"))
    }

    @Test
    fun `parse filters out HTML page URLs`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Web Page
            https://example.com/page.html
            #EXTINF:-1,Valid Stream
            https://stream.example.com/live.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.size)
        assertEquals("Valid Stream", result[0].title)
    }

    @Test
    fun `parse uses EXTGRP when group-title is empty`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Test Channel
            #EXTGRP:Sports
            https://stream.example.com/sports.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.size)
        assertEquals("Sports", result[0].group)
    }
}
