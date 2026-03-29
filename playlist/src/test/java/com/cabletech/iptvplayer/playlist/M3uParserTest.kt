package com.cabletech.iptvplayer.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    private val sampleM3u = """
        #EXTM3U
        #EXTINF:-1 tvg-id="cnn" tvg-name="CNN" tvg-logo="https://example.com/cnn.png" group-title="News",CNN
        http://stream.example.com/cnn
        #EXTINF:-1 group-title="Sports",ESPN
        http://stream.example.com/espn
        #EXTINF:-1,No Attrs Channel
        http://stream.example.com/noattrs
    """.trimIndent()

    @Test
    fun `parse returns correct channel count`() {
        val channels = M3uParser.parse(sampleM3u)
        assertEquals(3, channels.size)
    }

    @Test
    fun `parse extracts tvg attributes`() {
        val channel = M3uParser.parse(sampleM3u).first()
        assertEquals("cnn", channel.tvgId)
        assertEquals("CNN", channel.name)
        assertEquals("https://example.com/cnn.png", channel.logoUrl)
        assertEquals("News", channel.groupTitle)
        assertEquals("http://stream.example.com/cnn", channel.streamUrl)
    }

    @Test
    fun `parse falls back to display name when tvg-name absent`() {
        val channels = M3uParser.parse(sampleM3u)
        assertEquals("ESPN", channels[1].name)
    }

    @Test
    fun `parse handles missing logo gracefully`() {
        val channels = M3uParser.parse(sampleM3u)
        assertNull(channels[1].logoUrl)
    }

    @Test
    fun `parse handles channel with no attributes`() {
        val channels = M3uParser.parse(sampleM3u)
        assertEquals("No Attrs Channel", channels[2].name)
        assertNull(channels[2].groupTitle)
    }

    @Test
    fun `parse empty content returns empty list`() {
        assertEquals(emptyList<Any>(), M3uParser.parse(""))
    }
}
