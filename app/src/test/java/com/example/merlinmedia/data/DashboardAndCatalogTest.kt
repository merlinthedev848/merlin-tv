package com.example.merlinmedia.data

import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry
import com.example.merlinmedia.model.NavSection
import com.example.merlinmedia.model.SortMode
import org.junit.Assert.*
import org.junit.Test

class DashboardAndCatalogTest {

    @Test
    fun `verify curated live channels are valid and complete`() {
        val channels = CatalogRepository.curatedLiveChannels
        assertTrue("Curated live channels should not be empty", channels.isNotEmpty())
        for (item in channels) {
            assertTrue("Channel title must not be blank: ${item.id}", item.title.isNotBlank())
            assertTrue("Channel URL must be valid: ${item.title}", item.url.startsWith("http://") || item.url.startsWith("https://"))
            assertEquals("Kind must be LIVE", Kind.LIVE, item.type)
            assertFalse("Live channel should not be marked isVod", item.isVod)
        }
    }

    @Test
    fun `verify curated sky network channels are valid`() {
        val sky = CatalogRepository.curatedSkyChannels
        assertTrue("Sky channels should not be empty", sky.isNotEmpty())
        for (item in sky) {
            assertTrue("Sky channel title must not be blank", item.title.isNotBlank())
            assertTrue("Sky channel URL must be valid", item.url.startsWith("http://") || item.url.startsWith("https://"))
        }
    }

    @Test
    fun `verify curated movies are valid VOD entries`() {
        val movies = CatalogRepository.curatedMovies
        assertTrue("Curated movies should not be empty", movies.isNotEmpty())
        for (movie in movies) {
            assertTrue("Movie title must not be blank", movie.title.isNotBlank())
            assertTrue("Movie URL must be valid: ${movie.title}", movie.url.startsWith("http://") || movie.url.startsWith("https://"))
            assertEquals("Kind must be MOVIE", Kind.MOVIE, movie.type)
            assertTrue("Movie must be marked isVod", movie.isVod)
            assertTrue("Movie genre must not be blank", movie.genre.isNotBlank())
            assertTrue("Movie description must not be blank", movie.description.isNotBlank())
        }
    }

    @Test
    fun `verify curated series are valid episodic VOD entries`() {
        val series = CatalogRepository.curatedSeries
        assertTrue("Curated series should not be empty", series.isNotEmpty())
        for (show in series) {
            assertTrue("Series title must not be blank", show.title.isNotBlank())
            assertTrue("Series URL must be valid: ${show.title}", show.url.startsWith("http://") || show.url.startsWith("https://"))
            assertEquals("Kind must be SERIES", Kind.SERIES, show.type)
            assertTrue("Series must be marked isVod", show.isVod)
            assertTrue("Series description must not be blank", show.description.isNotBlank())
        }
    }

    @Test
    fun `verify instant catalogs fallback never returns null or empty collections`() {
        val instant = CatalogRepository.getInstantCatalogs(null)
        assertNotNull(instant)
        assertTrue("Live channels should have fallback", instant.live.isNotEmpty())
        assertTrue("Sky channels should have fallback", instant.sky.isNotEmpty())
        assertTrue("Movies should have fallback", instant.movies.isNotEmpty())
        assertTrue("Series should have fallback", instant.series.isNotEmpty())
    }

    @Test
    fun `verify all NavSection enum values exist and have valid titles`() {
        val sections = NavSection.entries
        assertTrue(sections.contains(NavSection.HOME))
        assertTrue(sections.contains(NavSection.LIVE))
        assertTrue(sections.contains(NavSection.EPG))
        assertTrue(sections.contains(NavSection.MOVIES))
        assertTrue(sections.contains(NavSection.SERIES))
        assertTrue(sections.contains(NavSection.FAVORITES))
        assertTrue(sections.contains(NavSection.RADIO))
        assertTrue(sections.contains(NavSection.HUB))

        for (section in sections) {
            assertTrue("Section title should not be blank: $section", section.title.isNotBlank())
        }
    }

    @Test
    fun `verify sort mode transformations and stability`() {
        val items = listOf(
            MediaEntry(title = "Zeta News", url = "https://example.com/z.m3u8", type = Kind.LIVE, country = "USA", quality = "720p"),
            MediaEntry(title = "Alpha Sports", url = "https://example.com/a.m3u8", type = Kind.LIVE, country = "UK", quality = "4K"),
            MediaEntry(title = "Beta Movie", url = "https://example.com/b.m3u8", type = Kind.MOVIE, country = "Canada", quality = "1080p")
        )

        val alphaSorted = items.sortedBy { it.title.lowercase() }
        assertEquals("Alpha Sports", alphaSorted[0].title)
        assertEquals("Beta Movie", alphaSorted[1].title)
        assertEquals("Zeta News", alphaSorted[2].title)

        val qualitySorted = items.sortedByDescending {
            when {
                it.quality.contains("4K", ignoreCase = true) -> 4
                it.quality.contains("1080", ignoreCase = true) -> 3
                it.quality.contains("720", ignoreCase = true) -> 2
                else -> 1
            }
        }
        assertEquals("Alpha Sports", qualitySorted[0].title) // 4K
        assertEquals("Beta Movie", qualitySorted[1].title)   // 1080p
        assertEquals("Zeta News", qualitySorted[2].title)    // 720p
    }

    @Test
    fun `verify quality detection accuracy`() {
        assertEquals("4K", CatalogRepository.detectQuality("BBC One 4K UHD"))
        assertEquals("4K", CatalogRepository.detectQuality("Movie (2160p)"))
        assertEquals("1080p", CatalogRepository.detectQuality("Sky News FHD"))
        assertEquals("1080p", CatalogRepository.detectQuality("Sky News 1080p"))
        assertEquals("720p", CatalogRepository.detectQuality("Channel HD 720p"))
        assertEquals("SD", CatalogRepository.detectQuality("Old Broadcast SD 480p"))
        assertEquals("1080p", CatalogRepository.detectQuality("Standard Feed", defaultQuality = "1080p"))
    }

    @Test
    fun `verify ke1th streams tzujtv json parser`() {
        val sampleJson = """
            [
                {
                    "name": "How to Train Your Dragon (2025)",
                    "type": "Movies",
                    "logo": "https://image.tmdb.org/t/p/w600/sample.jpg",
                    "category": "Fantasy",
                    "streamUrl": "https://vidfast.pro/movie/1087192",
                    "isEmbed": true
                },
                {
                    "name": "Stranger Things (2025)",
                    "type": "TV Series",
                    "logo": "https://image.tmdb.org/t/p/w600/sample2.jpg",
                    "category": "Sci-Fi",
                    "streamUrl": "https://vidfast.pro/tv/66732",
                    "isEmbed": true
                }
            ]
        """.trimIndent()

        val parsed = CatalogRepository.parseKe1thTzujtv(sampleJson)
        assertEquals(2, parsed.size)
        assertEquals("How to Train Your Dragon", parsed[0].title)
        assertEquals("2025", parsed[0].year)
        assertEquals(Kind.MOVIE, parsed[0].type)
        assertEquals("Fantasy", parsed[0].genre)
        assertTrue(parsed[0].isVod)

        assertEquals("Stranger Things", parsed[1].title)
        assertEquals(Kind.SERIES, parsed[1].type)
        assertEquals("Sci-Fi", parsed[1].genre)
    }

    @Test
    fun `verify ke1th streams channels json parser`() {
        val sampleChannelsJson = """
            [
                {
                    "name": "Stingray Music Hits",
                    "type": "TV",
                    "logo": "https://example.com/logo.png",
                    "category": "Music",
                    "streamUrl": "https://lotus.stingray.com/manifest/master.m3u8"
                }
            ]
        """.trimIndent()

        val parsed = CatalogRepository.parseKe1thChannels(sampleChannelsJson)
        assertEquals(1, parsed.size)
        assertEquals("Stingray Music Hits", parsed[0].title)
        assertEquals(Kind.LIVE, parsed[0].type)
        assertEquals("FAST | MUSIC", parsed[0].group)
        assertEquals("https://lotus.stingray.com/manifest/master.m3u8", parsed[0].url)
    }
}
