package com.cadnative.firevisioniptv.data.source.remote.playlist

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uDataSourceTest {

    private val dataSource = M3uDataSource(mockk(relaxed = true))

    @Test
    fun `parses channels with attributes and url-tvg header`() {
        val m3u = """
            #EXTM3U url-tvg="http://epg.example.com/guide.xml"
            #EXTINF:-1 tvg-id="CNN.us" tvg-logo="http://logo/cnn.png" group-title="News",CNN International
            http://stream.example.com/cnn.m3u8
            #EXTINF:-1 tvg-id="BBC.uk" group-title="News",BBC One
            http://stream.example.com/bbc.ts
        """.trimIndent()

        val result = dataSource.parse(m3u)

        assertEquals("http://epg.example.com/guide.xml", result.epgUrl)
        assertEquals(2, result.channels.size)

        val cnn = result.channels[0]
        assertEquals("CNN International", cnn.name)
        assertEquals("http://stream.example.com/cnn.m3u8", cnn.url)
        assertEquals("CNN.us", cnn.tvgId)
        assertEquals("News", cnn.groupTitle)
        assertEquals("http://logo/cnn.png", cnn.tvgLogo)

        val bbc = result.channels[1]
        assertEquals("BBC One", bbc.name)
        assertEquals("BBC.uk", bbc.tvgId)
    }

    @Test
    fun `handles missing epg header and skips stray tags`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1,Local Channel
            #EXTVLCOPT:network-caching=1000
            http://stream.example.com/local.m3u8
        """.trimIndent()

        val result = dataSource.parse(m3u)

        assertNull(result.epgUrl)
        assertEquals(1, result.channels.size)
        assertEquals("Local Channel", result.channels[0].name)
        assertEquals("http://stream.example.com/local.m3u8", result.channels[0].url)
    }

    @Test
    fun `empty playlist yields no channels`() {
        val result = dataSource.parse("")
        assertTrue(result.channels.isEmpty())
        assertNull(result.epgUrl)
    }

    @Test
    fun `channel ids are unique across duplicate tvg-ids`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="dup",A
            http://a
            #EXTINF:-1 tvg-id="dup",B
            http://b
        """.trimIndent()

        val result = dataSource.parse(m3u)
        assertEquals(2, result.channels.size)
        assertTrue(result.channels[0].id != result.channels[1].id)
    }
}
