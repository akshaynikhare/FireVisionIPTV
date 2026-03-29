package com.cabletech.iptvplayer.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class XmltvParserTest {

    private val sampleXmltv = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE tv SYSTEM "xmltv.dtd">
        <tv>
          <channel id="bbc1.uk">
            <display-name>BBC One</display-name>
          </channel>
          <programme start="20260330140000 +0000" stop="20260330150000 +0000" channel="bbc1.uk">
            <title>News at Two</title>
            <desc>The latest national and international headlines.</desc>
          </programme>
          <programme start="20260330150000 +0000" stop="20260330160000 +0000" channel="bbc1.uk">
            <title>Afternoon Drama</title>
          </programme>
          <programme start="20260330160000 +0000" stop="20260330170000 +0000" channel="cnn.us">
            <title>World News</title>
          </programme>
        </tv>
    """.trimIndent()

    private fun parseXml() = XmltvParser.parse(ByteArrayInputStream(sampleXmltv.toByteArray()))

    @Test
    fun `parse returns correct programme count`() {
        assertEquals(3, parseXml().size)
    }

    @Test
    fun `parse extracts title and channel id`() {
        val prog = parseXml().first()
        assertEquals("bbc1.uk", prog.channelId)
        assertEquals("News at Two", prog.title)
    }

    @Test
    fun `parse extracts description`() {
        val prog = parseXml().first()
        assertNotNull(prog.description)
        assertTrue(prog.description!!.isNotBlank())
    }

    @Test
    fun `parse handles missing description`() {
        val prog = parseXml()[1]
        assertEquals("Afternoon Drama", prog.title)
        assertEquals(null, prog.description)
    }

    @Test
    fun `parse start and stop times are epoch millis`() {
        val prog = parseXml().first()
        // 20260330140000 UTC -> must be a reasonable epoch value
        assertTrue("start time should be positive", prog.startUtcMs > 0)
        assertTrue("stop > start", prog.stopUtcMs > prog.startUtcMs)
    }

    @Test
    fun `parse handles multiple channels`() {
        val programmes = parseXml()
        val channelIds = programmes.map { it.channelId }.toSet()
        assertEquals(setOf("bbc1.uk", "cnn.us"), channelIds)
    }

    @Test
    fun `parse empty document returns empty list`() {
        val empty = "<tv></tv>"
        val result = XmltvParser.parse(ByteArrayInputStream(empty.toByteArray()))
        assertTrue(result.isEmpty())
    }
}
