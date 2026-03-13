package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetChannelByIdUseCase.
 *
 * Tests verify that:
 * - Use case correctly delegates to repository with channel ID
 * - Success results are properly returned
 * - Error results are properly returned for non-existent channels
 * - Flow emissions work correctly for channel updates
 */
class GetChannelByIdUseCaseTest {
    
    private lateinit var channelRepository: ChannelRepository
    private lateinit var getChannelByIdUseCase: GetChannelByIdUseCase
    
    @Before
    fun setup() {
        channelRepository = mockk()
        getChannelByIdUseCase = GetChannelByIdUseCase(channelRepository)
    }
    
    @Test
    fun `invoke returns success result with channel from repository`() = runTest {
        // Given
        val channelId = "channel-123"
        val expectedChannel = Channel(
            id = channelId,
            name = "Test Channel",
            streamUrl = "http://example.com/stream",
            logoUrl = "http://example.com/logo.png",
            category = "Sports",
            language = "English",
            country = "US",
            isFavorite = false
        )
        every { channelRepository.getChannelById(channelId) } returns flow {
            emit(Result.Success(expectedChannel))
        }
        
        // When
        val result = getChannelByIdUseCase(channelId).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedChannel, (result as Result.Success).data)
        verify(exactly = 1) { channelRepository.getChannelById(channelId) }
    }
    
    @Test
    fun `invoke returns error result when channel not found`() = runTest {
        // Given
        val channelId = "non-existent-id"
        val expectedException = Exception("Channel not found: $channelId")
        every { channelRepository.getChannelById(channelId) } returns flow {
            emit(Result.Error(expectedException))
        }
        
        // When
        val result = getChannelByIdUseCase(channelId).first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(expectedException, (result as Result.Error).exception)
        verify(exactly = 1) { channelRepository.getChannelById(channelId) }
    }
    
    @Test
    fun `invoke emits updates when channel favorite status changes`() = runTest {
        // Given
        val channelId = "channel-123"
        val channelNotFavorite = Channel(
            id = channelId,
            name = "Test Channel",
            streamUrl = "http://example.com/stream",
            logoUrl = null,
            category = "Sports",
            language = null,
            country = null,
            isFavorite = false
        )
        val channelFavorite = channelNotFavorite.copy(isFavorite = true)
        
        every { channelRepository.getChannelById(channelId) } returns flow {
            emit(Result.Success(channelNotFavorite))
            emit(Result.Success(channelFavorite))
        }
        
        // When
        val results = getChannelByIdUseCase(channelId).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Success)
        assertEquals(false, (results[0] as Result.Success).data.isFavorite)
        assertTrue(results[1] is Result.Success)
        assertEquals(true, (results[1] as Result.Success).data.isFavorite)
        verify(exactly = 1) { channelRepository.getChannelById(channelId) }
    }
    
    @Test
    fun `invoke handles channel with all optional fields populated`() = runTest {
        // Given
        val channelId = "full-channel"
        val fullChannel = Channel(
            id = channelId,
            name = "Full Channel",
            streamUrl = "http://example.com/stream",
            logoUrl = "http://example.com/logo.png",
            category = "Entertainment",
            language = "Spanish",
            country = "ES",
            isFavorite = true
        )
        every { channelRepository.getChannelById(channelId) } returns flow {
            emit(Result.Success(fullChannel))
        }
        
        // When
        val result = getChannelByIdUseCase(channelId).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channel = (result as Result.Success).data
        assertEquals("Full Channel", channel.name)
        assertEquals("http://example.com/logo.png", channel.logoUrl)
        assertEquals("Spanish", channel.language)
        assertEquals("ES", channel.country)
        assertEquals(true, channel.isFavorite)
        verify(exactly = 1) { channelRepository.getChannelById(channelId) }
    }
    
    @Test
    fun `invoke handles channel with minimal fields`() = runTest {
        // Given
        val channelId = "minimal-channel"
        val minimalChannel = Channel(
            id = channelId,
            name = "Minimal Channel",
            streamUrl = "http://example.com/stream",
            logoUrl = null,
            category = "Uncategorized",
            language = null,
            country = null,
            isFavorite = false
        )
        every { channelRepository.getChannelById(channelId) } returns flow {
            emit(Result.Success(minimalChannel))
        }
        
        // When
        val result = getChannelByIdUseCase(channelId).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channel = (result as Result.Success).data
        assertEquals("Minimal Channel", channel.name)
        assertEquals(null, channel.logoUrl)
        assertEquals(null, channel.language)
        assertEquals(null, channel.country)
        verify(exactly = 1) { channelRepository.getChannelById(channelId) }
    }
    
    @Test
    fun `invoke passes correct channel ID to repository`() = runTest {
        // Given
        val channelIds = listOf("id1", "id2", "id3")
        channelIds.forEach { id ->
            every { channelRepository.getChannelById(id) } returns flow {
                emit(Result.Success(Channel(
                    id = id,
                    name = "Channel $id",
                    streamUrl = "http://example.com/$id",
                    logoUrl = null,
                    category = "Test",
                    language = null,
                    country = null,
                    isFavorite = false
                )))
            }
        }
        
        // When & Then
        channelIds.forEach { id ->
            val result = getChannelByIdUseCase(id).first()
            assertTrue(result is Result.Success)
            assertEquals(id, (result as Result.Success).data.id)
            verify(exactly = 1) { channelRepository.getChannelById(id) }
        }
    }
}
