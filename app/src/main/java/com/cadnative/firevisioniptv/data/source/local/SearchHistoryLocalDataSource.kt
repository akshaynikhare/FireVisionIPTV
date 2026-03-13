package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.SearchHistoryDao
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for search history database operations.
 * 
 * This class wraps the SearchHistoryDao and provides a clean interface for
 * the repository layer to interact with the local database. All database
 * operations are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: US-003.3 (Recent searches saved locally)
 */
@Singleton
class SearchHistoryLocalDataSource @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get recent search queries ordered by timestamp (most recent first).
     * 
     * @param limit Maximum number of recent searches to return
     * @return Flow emitting list of recent search history entries
     */
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches(limit)
    }
    
    /**
     * Save a search query to history.
     * 
     * @param searchHistory The search history entry to save
     */
    suspend fun saveSearch(searchHistory: SearchHistoryEntity) = withContext(dispatcher) {
        searchHistoryDao.saveSearch(searchHistory)
        // Clean up old entries to prevent unbounded growth
        searchHistoryDao.deleteOldSearches(keepCount = 50)
    }
    
    /**
     * Clear all search history.
     */
    suspend fun clearHistory() = withContext(dispatcher) {
        searchHistoryDao.clearHistory()
    }
}
