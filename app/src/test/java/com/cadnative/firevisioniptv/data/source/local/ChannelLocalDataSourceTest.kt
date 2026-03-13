package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChannelLocalDataSource.
 * 
 * Tests the wrapping of ChannelDao operations and proper dispatcher usage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelLocalDataSourceTest {
    
    private lateinit var channelDao: ChannelDao
    private lateinit var channelLocalDataSource: ChannelLocalDataSource
    
    @Before
    fun setup() {
        channelDao = mockk()
        channelLocalDataSource = ChannelLocalDataSource(channelDao, Dispatchers.Unconfined)
    }
    
    @Test
    fun `getAllChannels returns flow from dao`() = runTest {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        every { channelDao.getAllChannels() } returns flowOf(channels)
        
        // When
        val result = channelLocalDataSource.getAllChannels().first()
        
        // Then
        assertEquals(channels, result)
        coVerify { channelDao.getAllChannels() }
    }
    
    @Test
    fun `getChannelById returns flow from dao`() = runTest {
        // Given
        val channelId = "1"
        val channel = ChannelEntity(
            id = channelId,
            name = "Channel 1",
            streamUrl = "http://example.com/1",
            logoUrl = null,
            categoryId = "cat1",
            language = "en",
            country = "US",
            groupTitle = "Group 1",
            tvgId = "tvg1",
            tvgName = "TVG 1",
            isActive = true
        )
        every { channelDao.getChannelById(channelId) } returns flowOf(channel)
        
        // When
        val result = channelLocalDataSource.getChannelById(channelId).first()
        
        // Then
        assertEquals(channel, result)
        coVerify { channelDao.getChannelById(channelId) }
    }
    
    @Test
    fun `getChannelById returns null when not found`() = runTest {
        // Given
        val channelId = "999"
        every { channelDao.getChannelById(channelId) } returns flowOf(null)
        
        // When
        val result = channelLocalDataSource.getChannelById(channelId).first()
        
        // Then
        assertNull(result)
        coVerify { channelDao.getChannelById(channelId) }
    }
    
    @Test
    fun `getChannelsByCategory returns flow from dao`() = runTest {
        // Given
        val categoryId = "cat1"
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = categoryId,
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        every { channelDao.getChannelsByCategory(categoryId) } returns flowOf(channels)
        
        // When
        val result = channelLocalDataSource.getChannelsByCategory(categoryId).first()
        
        // Then
        assertEquals(channels, result)
        coVerify { channelDao.getChannelsByCategory(categoryId) }
    }
    
    @Test
    fun `searchChannels returns flow from dao`() = runTest {
        // Given
        val query = "test"
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Test Channel",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        every { channelDao.searchChannels(query) } returns flowOf(channels)
        
        // When
        val result = channelLocalDataSource.searchChannels(query).first()
        
        // Then
        assertEquals(channels, result)
        coVerify { channelDao.searchChannels(query) }
    }
    
    @Test
    fun `insertChannels calls dao`() = runTest {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        coEvery { channelDao.insertChannels(channels) } returns Unit
        
        // When
        channelLocalDataSource.insertChannels(channels)
        
        // Then
        coVerify { channelDao.insertChannels(channels) }
    }
    
    @Test
    fun `deleteAllChannels calls dao`() = runTest {
        // Given
        coEvery { channelDao.deleteAllChannels() } returns Unit
        
        // When
        channelLocalDataSource.deleteAllChannels()
        
        // Then
        coVerify { channelDao.deleteAllChannels() }
    }
    
    @Test
    fun `replaceAllChannels calls dao`() = runTest {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = null,
                categoryId = "cat1",
                language = "en",
                country = "US",
                groupTitle = "Group 1",
                tvgId = "tvg1",
                tvgName = "TVG 1",
                isActive = true
            )
        )
        coEvery { channelDao.replaceAllChannels(channels) } returns Unit
        
        // When
        channelLocalDataSource.replaceAllChannels(channels)
        
        // Then
        coVerify { channelDao.replaceAllChannels(channels) }
    }
}
