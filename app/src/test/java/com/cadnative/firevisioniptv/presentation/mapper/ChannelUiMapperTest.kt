package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChannelUiMapperTest {

    private lateinit var mapper: ChannelUiMapper

    @Before
    fun setup() {
        mapper = ChannelUiMapper()
    }

    private fun createChannel(
        id: String = "ch1",
        name: String = "Test Channel",
        streamUrl: String = "http://stream.test/live.m3u8",
        logoUrl: String? = "http://logo.test/img.png",
        category: String = "News",
        language: String? = "English",
        country: String? = "US",
        tvgId: String? = "test.tvg",
        isFavorite: Boolean = false,
        alternateStreamUrls: List<String> = emptyList()
    ) = Channel(id, name, streamUrl, logoUrl, category, language, country, tvgId, isFavorite, alternateStreamUrls)

    @Test
    fun `toUiModel maps all fields with defaults`() {
        val channel = createChannel()
        val result = mapper.toUiModel(channel)

        assertEquals("ch1", result.id)
        assertEquals("Test Channel", result.name)
        assertEquals("http://logo.test/img.png", result.logoUrl)
        assertEquals("http://stream.test/live.m3u8", result.streamUrl)
        assertEquals("News", result.category)
        assertEquals("test.tvg", result.tvgId)
        assertEquals(false, result.isFavorite)
        assertEquals(ChannelHealthStatus.UNKNOWN, result.healthStatus)
        assertNull(result.thumbnailPath)
        assertEquals(emptyList<String>(), result.alternateStreamUrls)
    }

    @Test
    fun `toUiModel maps explicit health and thumbnail`() {
        val channel = createChannel(isFavorite = true, alternateStreamUrls = listOf("http://alt1", "http://alt2"))
        val result = mapper.toUiModel(channel, ChannelHealthStatus.ONLINE, "/path/thumb.jpg")

        assertEquals(ChannelHealthStatus.ONLINE, result.healthStatus)
        assertEquals("/path/thumb.jpg", result.thumbnailPath)
        assertEquals(true, result.isFavorite)
        assertEquals(listOf("http://alt1", "http://alt2"), result.alternateStreamUrls)
    }

    @Test
    fun `toUiModelsWithHealth matches health by channelId`() {
        val channels = listOf(
            createChannel(id = "ch1", name = "Channel 1"),
            createChannel(id = "ch2", name = "Channel 2")
        )
        val health = listOf(
            ChannelHealthEntity(channelId = "ch1", status = "ONLINE", thumbnailPath = "/thumb1.jpg"),
            ChannelHealthEntity(channelId = "ch2", status = "OFFLINE")
        )

        val result = mapper.toUiModelsWithHealth(channels, health)

        assertEquals(2, result.size)
        assertEquals(ChannelHealthStatus.ONLINE, result[0].healthStatus)
        assertEquals("/thumb1.jpg", result[0].thumbnailPath)
        assertEquals(ChannelHealthStatus.OFFLINE, result[1].healthStatus)
        assertNull(result[1].thumbnailPath)
    }

    @Test
    fun `toUiModelsWithHealth defaults to UNKNOWN for unmatched channels`() {
        val channels = listOf(createChannel(id = "ch1"))
        val health = listOf(ChannelHealthEntity(channelId = "ch999", status = "ONLINE"))

        val result = mapper.toUiModelsWithHealth(channels, health)

        assertEquals(1, result.size)
        assertEquals(ChannelHealthStatus.UNKNOWN, result[0].healthStatus)
    }

    @Test
    fun `toUiModelsWithHealth handles invalid status string`() {
        val channels = listOf(createChannel(id = "ch1"))
        val health = listOf(ChannelHealthEntity(channelId = "ch1", status = "INVALID_STATUS"))

        val result = mapper.toUiModelsWithHealth(channels, health)

        assertEquals(ChannelHealthStatus.UNKNOWN, result[0].healthStatus)
    }

    @Test
    fun `toUiModelsWithHealth handles empty lists`() {
        val result = mapper.toUiModelsWithHealth(emptyList(), emptyList())
        assertEquals(emptyList<ChannelUiModel>(), result)
    }

    @Test
    fun `fromUiModel maps fields and nulls language and country`() {
        val uiModel = ChannelUiModel(
            id = "ch1",
            name = "Test",
            logoUrl = "http://logo.png",
            streamUrl = "http://stream.m3u8",
            category = "Sports",
            isFavorite = true,
            tvgId = "tvg.1"
        )

        val result = mapper.fromUiModel(uiModel)

        assertEquals("ch1", result.id)
        assertEquals("Test", result.name)
        assertEquals("http://stream.m3u8", result.streamUrl)
        assertEquals("http://logo.png", result.logoUrl)
        assertEquals("Sports", result.category)
        assertEquals(true, result.isFavorite)
        assertNull(result.language)
        assertNull(result.country)
    }

    @Test
    fun `fromUiModel handles null streamUrl as empty string`() {
        val uiModel = ChannelUiModel(
            id = "ch1",
            name = "Test",
            logoUrl = null,
            streamUrl = null,
            category = "News",
            isFavorite = false
        )

        val result = mapper.fromUiModel(uiModel)

        assertEquals("", result.streamUrl)
        assertNull(result.logoUrl)
    }
}
