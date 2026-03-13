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
 * Unit tests for GetChannelsByCategoryUseCase.
 *
 * Tests verify that:
 * - Use case correctly delegates to repository with category filter
 * - Success results are properly returned with filtered channels
 * - Error results are properly returned
 * - Empty results are handled for categories with no channels
 * - Flow emissions work correctly
 */
class GetChannelsByCategoryUseCaseTest {
    
    private lateinit var channelRepository: ChannelRepository
    private lateinit var getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase
    
    @Before
    fun setup() {
        channelRepository = mockk()
        getChannelsByCategoryUseCase = GetChannelsByCategoryUseCase(channelRepository)
    }
    
    @Test
    fun `invoke returns success result with channels from specified category`() = runTest {
        // Given
        val category = "Sports"
        val expectedChannels = listOf(
            Channel(
                id = "1",
                name = "Sports Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = "http://example.com/logo1.png",
                category = category,
                language = "English",
                country = "US",
                isFavorite = false
            ),
            Channel(
                id = "2",
                name = "Sports Channel 2",
                streamUrl = "http://example.com/stream2",
                logoUrl = "http://example.com/logo2.png",
                category = category,
                language = "English",
                country = "US",
                isFavorite = true
            )
        )
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Success(expectedChannels))
        }
        
        // When
        val result = getChannelsByCategoryUseCase(category).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(2, channels.size)
        assertTrue(channels.all { it.category == category })
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
    
    @Test
    fun `invoke returns empty list when category has no channels`() = runTest {
        // Given
        val category = "EmptyCategory"
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Success(emptyList()))
        }
        
        // When
        val result = getChannelsByCategoryUseCase(category).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(emptyList<Channel>(), (result as Result.Success).data)
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
    
    @Test
    fun `invoke returns error result when repository fails`() = runTest {
        // Given
        val category = "Sports"
        val expectedException = Exception("Database error")
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Error(expectedException))
        }
        
        // When
        val result = getChannelsByCategoryUseCase(category).first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(expectedException, (result as Result.Error).exception)
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
    
    @Test
    fun `invoke emits multiple updates when category channels change`() = runTest {
        // Given
        val category = "News"
        val firstChannels = listOf(
            Channel(
                id = "1",
                name = "News Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = null,
                category = category,
                language = null,
                country = null,
                isFavorite = false
            )
        )
        val secondChannels = firstChannels + Channel(
            id = "2",
            name = "News Channel 2",
            streamUrl = "http://example.com/stream2",
            logoUrl = null,
            category = category,
            language = null,
            country = null,
            isFavorite = false
        )
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Success(firstChannels))
            emit(Result.Success(secondChannels))
        }
        
        // When
        val results = getChannelsByCategoryUseCase(category).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Success)
        assertEquals(1, (results[0] as Result.Success).data.size)
        assertTrue(results[1] is Result.Success)
        assertEquals(2, (results[1] as Result.Success).data.size)
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
    
    @Test
    fun `invoke handles different category names correctly`() = runTest {
        // Given
        val categories = listOf("Sports", "News", "Entertainment", "Movies")
        categories.forEach { category ->
            every { channelRepository.getChannelsByCategory(category) } returns flow {
                emit(Result.Success(listOf(
                    Channel(
                        id = "channel-$category",
                        name = "$category Channel",
                        streamUrl = "http://example.com/$category",
                        logoUrl = null,
                        category = category,
                        language = null,
                        country = null,
                        isFavorite = false
                    )
                )))
            }
        }
        
        // When & Then
        categories.forEach { category ->
            val result = getChannelsByCategoryUseCase(category).first()
            assertTrue(result is Result.Success)
            val channels = (result as Result.Success).data
            assertEquals(1, channels.size)
            assertEquals(category, channels[0].category)
            verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
        }
    }
    
    @Test
    fun `invoke handles category with mixed favorite status`() = runTest {
        // Given
        val category = "Entertainment"
        val mixedChannels = listOf(
            Channel(
                id = "1",
                name = "Channel 1",
                streamUrl = "http://example.com/stream1",
                logoUrl = null,
                category = category,
                language = null,
                country = null,
                isFavorite = true
            ),
            Channel(
                id = "2",
                name = "Channel 2",
                streamUrl = "http://example.com/stream2",
                logoUrl = null,
                category = category,
                language = null,
                country = null,
                isFavorite = false
            ),
            Channel(
                id = "3",
                name = "Channel 3",
                streamUrl = "http://example.com/stream3",
                logoUrl = null,
                category = category,
                language = null,
                country = null,
                isFavorite = true
            )
        )
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Success(mixedChannels))
        }
        
        // When
        val result = getChannelsByCategoryUseCase(category).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(3, channels.size)
        assertEquals(2, channels.count { it.isFavorite })
        assertEquals(1, channels.count { !it.isFavorite })
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
    
    @Test
    fun `invoke passes correct category to repository`() = runTest {
        // Given
        val category = "TestCategory"
        every { channelRepository.getChannelsByCategory(category) } returns flow {
            emit(Result.Success(emptyList()))
        }
        
        // When
        getChannelsByCategoryUseCase(category).first()
        
        // Then
        verify(exactly = 1) { channelRepository.getChannelsByCategory(category) }
    }
}
