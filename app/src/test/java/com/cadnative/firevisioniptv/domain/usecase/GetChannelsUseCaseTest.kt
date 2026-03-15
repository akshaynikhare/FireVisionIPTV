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
 * Unit tests for GetChannelsUseCase.
 *
 * Tests verify that:
 * - Use case correctly delegates to repository
 * - Success results are properly returned
 * - Error results are properly returned
 * - Flow emissions work correctly
 */
class GetChannelsUseCaseTest {
    
    private lateinit var channelRepository: ChannelRepository
    private lateinit var getChannelsUseCase: GetChannelsUseCase
    
    @Before
    fun setup() {
        channelRepository = mockk()
        getChannelsUseCase = GetChannelsUseCase(channelRepository)
    }
    
    @Test
    fun `invoke returns success result with channels from repository`() = runTest {
        // Given
        val expectedChannels = listOf(
            Channel(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = "http://example.com/logo1.png",
                category = "Sports",
                language = "English",
                country = "US",
                isFavorite = false
            ),
            Channel(
                id = "2",
                name = "Channel 2",
                streamUrl = "http://example.com/stream2",
                logoUrl = "http://example.com/logo2.png",
                category = "News",
                language = "English",
                country = "US",
                isFavorite = true
            )
        )
        every { channelRepository.getChannels() } returns flow {
            emit(Result.Success(expectedChannels))
        }
        
        // When
        val result = getChannelsUseCase(Unit).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedChannels, (result as Result.Success).data)
        verify(exactly = 1) { channelRepository.getChannels() }
    }
    
    @Test
    fun `invoke returns error result when repository fails`() = runTest {
        // Given
        val expectedException = Exception("Network error")
        every { channelRepository.getChannels() } returns flow {
            emit(Result.Error(expectedException))
        }
        
        // When
        val result = getChannelsUseCase(Unit).first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(expectedException, (result as Result.Error).exception)
        verify(exactly = 1) { channelRepository.getChannels() }
    }
    
    @Test
    fun `invoke returns empty list when no channels available`() = runTest {
        // Given
        every { channelRepository.getChannels() } returns flow {
            emit(Result.Success(emptyList()))
        }
        
        // When
        val result = getChannelsUseCase(Unit).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(emptyList<Channel>(), (result as Result.Success).data)
        verify(exactly = 1) { channelRepository.getChannels() }
    }
    
    @Test
    fun `invoke emits multiple updates from repository flow`() = runTest {
        // Given
        val firstChannels = listOf(
            Channel(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = null,
                category = "Sports",
                language = null,
                country = null,
                isFavorite = false
            )
        )
        val secondChannels = listOf(
            Channel(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = null,
                category = "Sports",
                language = null,
                country = null,
                isFavorite = false
            ),
            Channel(
                id = "2",
                name = "Channel 2",
                streamUrl = "http://example.com/stream2",
                logoUrl = null,
                category = "News",
                language = null,
                country = null,
                isFavorite = false
            )
        )
        every { channelRepository.getChannels() } returns flow {
            emit(Result.Success(firstChannels))
            emit(Result.Success(secondChannels))
        }
        
        // When
        val results = getChannelsUseCase(Unit).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Success)
        assertEquals(1, (results[0] as Result.Success).data.size)
        assertTrue(results[1] is Result.Success)
        assertEquals(2, (results[1] as Result.Success).data.size)
        verify(exactly = 1) { channelRepository.getChannels() }
    }
    
    @Test
    fun `invoke handles channels with all optional fields null`() = runTest {
        // Given
        val channelsWithNulls = listOf(
            Channel(
                id = "1",
                name = "Minimal Channel",
                streamUrl = "http://example.com/stream",
                logoUrl = null,
                category = "Uncategorized",
                language = null,
                country = null,
                isFavorite = false
            )
        )
        every { channelRepository.getChannels() } returns flow {
            emit(Result.Success(channelsWithNulls))
        }
        
        // When
        val result = getChannelsUseCase(Unit).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(1, channels.size)
        assertEquals(null, channels[0].logoUrl)
        assertEquals(null, channels[0].language)
        assertEquals(null, channels[0].country)
        verify(exactly = 1) { channelRepository.getChannels() }
    }
}
