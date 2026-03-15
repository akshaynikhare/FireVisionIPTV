package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.SearchHistoryLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SearchHistoryRepositoryImpl.
 * 
 * Tests the search history management functionality including:
 * - Saving searches to local database
 * - Retrieving recent searches ordered by timestamp
 * - Clearing history
 * - Error handling
 * 
 * Requirements: US-003.3 (Recent searches saved locally), US-003.6 (Clear search history)
 */
class SearchHistoryRepositoryImplTest {
    
    private lateinit var repository: SearchHistoryRepositoryImpl
    private lateinit var localDataSource: SearchHistoryLocalDataSource
    private val testDispatcher = StandardTestDispatcher()
    
    private val testSearchHistory1 = SearchHistoryEntity(
        id = 1,
        query = "sports",
        timestamp = System.currentTimeMillis() - 1000
    )
    
    private val testSearchHistory2 = SearchHistoryEntity(
        id = 2,
        query = "news",
        timestamp = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        localDataSource = mockk()
        
        repository = SearchHistoryRepositoryImpl(
            localDataSource = localDataSource,
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getRecentSearches returns searches ordered by timestamp`() = runTest(testDispatcher) {
        // Given - most recent first
        val searchHistory = listOf(testSearchHistory2, testSearchHistory1)
        every { localDataSource.getRecentSearches(10) } returns flowOf(searchHistory)
        
        // When
        val result = repository.getRecentSearches(10).first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals("news", data[0]) // Most recent
        assertEquals("sports", data[1])
    }

    
    @Test
    fun `getRecentSearches with custom limit returns correct number`() = runTest(testDispatcher) {
        // Given
        val searchHistory = listOf(testSearchHistory2)
        every { localDataSource.getRecentSearches(1) } returns flowOf(searchHistory)
        
        // When
        val result = repository.getRecentSearches(1).first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("news", data[0])
    }
    
    @Test
    fun `getRecentSearches returns empty list when no history`() = runTest(testDispatcher) {
        // Given
        every { localDataSource.getRecentSearches(10) } returns flowOf(emptyList())
        
        // When
        val result = repository.getRecentSearches(10).first()
        
        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.isEmpty())
    }
    
    @Test
    fun `getRecentSearches handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        every { localDataSource.getRecentSearches(10) } throws exception
        
        // When
        val result = repository.getRecentSearches(10).first()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `saveSearch saves query to local database`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.saveSearch(any()) } returns Unit
        
        // When
        val result = repository.saveSearch("sports")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            localDataSource.saveSearch(match { 
                it.query == "sports" 
            }) 
        }
    }
    
    @Test
    fun `saveSearch trims whitespace from query`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.saveSearch(any()) } returns Unit
        
        // When
        val result = repository.saveSearch("  sports  ")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { 
            localDataSource.saveSearch(match { 
                it.query == "sports" 
            }) 
        }
    }
    
    @Test
    fun `saveSearch ignores empty queries`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.saveSearch(any()) } returns Unit
        
        // When
        val result = repository.saveSearch("")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { localDataSource.saveSearch(any()) }
    }
    
    @Test
    fun `saveSearch ignores blank queries`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.saveSearch(any()) } returns Unit
        
        // When
        val result = repository.saveSearch("   ")
        
        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { localDataSource.saveSearch(any()) }
    }
    
    @Test
    fun `saveSearch handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        coEvery { localDataSource.saveSearch(any()) } throws exception
        
        // When
        val result = repository.saveSearch("sports")
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `clearHistory removes all searches from database`() = runTest(testDispatcher) {
        // Given
        coEvery { localDataSource.clearHistory() } returns Unit
        
        // When
        val result = repository.clearHistory()
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { localDataSource.clearHistory() }
    }
    
    @Test
    fun `clearHistory handles errors gracefully`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Database error")
        coEvery { localDataSource.clearHistory() } throws exception
        
        // When
        val result = repository.clearHistory()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `removeSearch returns success`() = runTest(testDispatcher) {
        // Given - removeSearch is not yet implemented
        
        // When
        val result = repository.removeSearch("sports")
        
        // Then
        assertTrue(result is Result.Success)
    }
}
