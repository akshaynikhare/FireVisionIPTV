package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.SearchFilter
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SearchChannelsUseCase.
 *
 * Tests verify that:
 * - Use case correctly delegates to repository with search query
 * - Filters are applied correctly to search results
 * - Multiple filters work with AND logic
 * - Combined filters work correctly
 * - Empty filters return all search results
 * - Error results are properly returned
 */
class SearchChannelsUseCaseTest {
    
    private lateinit var channelRepository: ChannelRepository
    private lateinit var searchChannelsUseCase: SearchChannelsUseCase
    
    private val testChannels = listOf(
        Channel(
            id = "1",
            name = "BBC News",
            streamUrl = "http://example.com/stream1",
            logoUrl = "http://example.com/logo1.png",
            category = "News",
            language = "English",
            country = "UK",
            isFavorite = false
        ),
        Channel(
            id = "2",
            name = "CNN News",
            streamUrl = "http://example.com/stream2",
            logoUrl = "http://example.com/logo2.png",
            category = "News",
            language = "English",
            country = "US",
            isFavorite = true
        ),
        Channel(
            id = "3",
            name = "France 24",
            streamUrl = "http://example.com/stream3",
            logoUrl = "http://example.com/logo3.png",
            category = "News",
            language = "French",
            country = "France",
            isFavorite = false
        ),
        Channel(
            id = "4",
            name = "ESPN Sports",
            streamUrl = "http://example.com/stream4",
            logoUrl = "http://example.com/logo4.png",
            category = "Sports",
            language = "English",
            country = "US",
            isFavorite = false
        )
    )
    
    @Before
    fun setup() {
        channelRepository = mockk()
        searchChannelsUseCase = SearchChannelsUseCase(channelRepository)
    }
    
    @Test
    fun `invoke returns all search results when no filters applied`() = runTest {
        // Given
        val query = "news"
        val params = SearchChannelsUseCase.Params(query = query, filters = emptyList())
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(4, channels.size)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke filters by category correctly`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(SearchFilter.ByCategory("News"))
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(3, channels.size)
        assertTrue(channels.all { it.category == "News" })
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke filters by language correctly`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(SearchFilter.ByLanguage("English"))
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(3, channels.size)
        assertTrue(channels.all { it.language == "English" })
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke filters by country correctly`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(SearchFilter.ByCountry("US"))
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(2, channels.size)
        assertTrue(channels.all { it.country == "US" })
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke applies multiple filters with AND logic`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(
            SearchFilter.ByCategory("News"),
            SearchFilter.ByLanguage("English")
        )
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(2, channels.size)
        assertTrue(channels.all { it.category == "News" && it.language == "English" })
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke applies combined filters correctly`() = runTest {
        // Given
        val query = "news"
        val combinedFilter = SearchFilter.Combined(
            filters = listOf(
                SearchFilter.ByCategory("News"),
                SearchFilter.ByCountry("US")
            )
        )
        val params = SearchChannelsUseCase.Params(query = query, filters = listOf(combinedFilter))
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(1, channels.size)
        assertEquals("CNN News", channels[0].name)
        assertTrue(channels[0].category == "News" && channels[0].country == "US")
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke returns empty list when no channels match filters`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(
            SearchFilter.ByCategory("News"),
            SearchFilter.ByCountry("Germany")
        )
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(0, channels.size)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke returns error result when repository fails`() = runTest {
        // Given
        val query = "news"
        val params = SearchChannelsUseCase.Params(query = query, filters = emptyList())
        val expectedException = Exception("Network error")
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Error(expectedException))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(expectedException, (result as Result.Error).exception)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke handles empty search results`() = runTest {
        // Given
        val query = "nonexistent"
        val params = SearchChannelsUseCase.Params(query = query, filters = emptyList())
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(emptyList()))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(emptyList<Channel>(), (result as Result.Success).data)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke applies three filters with AND logic`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(
            SearchFilter.ByCategory("News"),
            SearchFilter.ByLanguage("English"),
            SearchFilter.ByCountry("US")
        )
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(1, channels.size)
        assertEquals("CNN News", channels[0].name)
        assertTrue(
            channels[0].category == "News" &&
            channels[0].language == "English" &&
            channels[0].country == "US"
        )
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke handles nested combined filters`() = runTest {
        // Given
        val query = "news"
        val nestedFilter = SearchFilter.Combined(
            filters = listOf(
                SearchFilter.Combined(
                    filters = listOf(
                        SearchFilter.ByCategory("News"),
                        SearchFilter.ByLanguage("English")
                    )
                ),
                SearchFilter.ByCountry("UK")
            )
        )
        val params = SearchChannelsUseCase.Params(query = query, filters = listOf(nestedFilter))
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        assertEquals(1, channels.size)
        assertEquals("BBC News", channels[0].name)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke preserves favorite status in filtered results`() = runTest {
        // Given
        val query = "news"
        val filters = listOf(SearchFilter.ByCategory("News"))
        val params = SearchChannelsUseCase.Params(query = query, filters = filters)
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(testChannels))
        }
        
        // When
        val result = searchChannelsUseCase(params).first()
        
        // Then
        assertTrue(result is Result.Success)
        val channels = (result as Result.Success).data
        val favoriteChannels = channels.filter { it.isFavorite }
        assertEquals(1, favoriteChannels.size)
        assertEquals("CNN News", favoriteChannels[0].name)
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
    
    @Test
    fun `invoke passes correct query to repository`() = runTest {
        // Given
        val query = "specific search query"
        val params = SearchChannelsUseCase.Params(query = query, filters = emptyList())
        every { channelRepository.searchChannels(query) } returns flow {
            emit(Result.Success(emptyList()))
        }
        
        // When
        searchChannelsUseCase(params).first()
        
        // Then
        verify(exactly = 1) { channelRepository.searchChannels(query) }
    }
}
