package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.SearchHistoryDao
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
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SearchHistoryLocalDataSource.
 * 
 * Tests the local data source wrapper for search history operations,
 * ensuring proper delegation to the DAO and correct dispatcher usage.
 * 
 * Requirements: US-003.3 (Recent searches saved locally)
 */
class SearchHistoryLocalDataSourceTest {
    
    private lateinit var dataSource: SearchHistoryLocalDataSource
    private lateinit var searchHistoryDao: SearchHistoryDao
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
        searchHistoryDao = mockk()
        
        dataSource = SearchHistoryLocalDataSource(
            searchHistoryDao = searchHistoryDao,
            dispatcher = testDispatcher
        )
    }
    
    @Test
    fun `getRecentSearches returns flow from DAO`() = runTest(testDispatcher) {
        // Given
        val searchHistory = listOf(testSearchHistory2, testSearchHistory1)
        every { searchHistoryDao.getRecentSearches(10) } returns flowOf(searchHistory)
        
        // When
        val result = dataSource.getRecentSearches(10).first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals("news", result[0].query)
        assertEquals("sports", result[1].query)
    }
    
    @Test
    fun `getRecentSearches with custom limit delegates to DAO`() = runTest(testDispatcher) {
        // Given
        val searchHistory = listOf(testSearchHistory2)
        every { searchHistoryDao.getRecentSearches(5) } returns flowOf(searchHistory)
        
        // When
        val result = dataSource.getRecentSearches(5).first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("news", result[0].query)
    }
    
    @Test
    fun `saveSearch delegates to DAO and cleans up old entries`() = runTest(testDispatcher) {
        // Given
        coEvery { searchHistoryDao.saveSearch(any()) } returns Unit
        coEvery { searchHistoryDao.deleteOldSearches(50) } returns Unit
        
        // When
        dataSource.saveSearch(testSearchHistory1)
        
        // Then
        coVerify { searchHistoryDao.saveSearch(testSearchHistory1) }
        coVerify { searchHistoryDao.deleteOldSearches(50) }
    }
    
    @Test
    fun `clearHistory delegates to DAO`() = runTest(testDispatcher) {
        // Given
        coEvery { searchHistoryDao.clearHistory() } returns Unit
        
        // When
        dataSource.clearHistory()
        
        // Then
        coVerify { searchHistoryDao.clearHistory() }
    }
}
