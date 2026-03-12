package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChannelLocalDataSource.
 * 
 * Tests the wrapper functionality around ChannelDao to ensure
 * proper delegation and Flow-based query exposure.
 */
class ChannelLocalDataSourceTest {
    
    private lateinit var channelDao: ChannelDao
    private lateinit var channelLocalDataSource: ChannelLocalDataSource
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        channelDao = mockk()
        channelLocalDataSource = ChannelLocalDataSource(channelDao, testDispatcher)
    }
    
    @Test
    fun `getAllChannels returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
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
    }
    
    @Test
    fun `getChannelById returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val channelId = "1"
        val channel = ChannelEntity(
            id = channelId,
            name = "Channel 1",
            streamUrl = "http://example.com/1",
            logoUrl = "http://example.com/logo1.png",
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
    }
    
    @Test
    fun `getChannelById returns null when channel not found`() = runTest(testDispatcher) {
        // Given
        val channelId = "nonexistent"
        every { channelDao.getChannelById(channelId) } returns flowOf(null)
        
        // When
        val result = channelLocalDataSource.getChannelById(channelId).first()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getChannelsByCategory returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val categoryId = "cat1"
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
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
    }
    
    @Test
    fun `searchChannels returns flow from dao`() = runTest(testDispatcher) {
        // Given
        val query = "test"
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Test Channel",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
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
    }
    
    @Test
    fun `insertChannels delegates to dao`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
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
    fun `deleteAllChannels delegates to dao`() = runTest(testDispatcher) {
        // Given
        coEvery { channelDao.deleteAllChannels() } returns Unit
        
        // When
        channelLocalDataSource.deleteAllChannels()
        
        // Then
        coVerify { channelDao.deleteAllChannels() }
    }
    
    @Test
    fun `replaceAllChannels delegates to dao`() = runTest(testDispatcher) {
        // Given
        val channels = listOf(
            ChannelEntity(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/1",
                logoUrl = "http://example.com/logo1.png",
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
