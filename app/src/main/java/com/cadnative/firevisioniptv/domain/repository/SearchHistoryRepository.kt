package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for search history operations.
 * 
 * This interface defines the contract for managing search history,
 * including saving searches and retrieving recent queries.
 */
interface SearchHistoryRepository {
    
    /**
     * Get recent search queries.
     * 
     * Returns the most recent search queries, ordered by timestamp descending.
     * 
     * @param limit Maximum number of recent searches to return
     * @return Flow emitting Result with list of recent search queries
     */
    fun getRecentSearches(limit: Int = 10): Flow<Result<List<String>>>
    
    /**
     * Save a search query to history.
     * 
     * Adds the query to search history with current timestamp.
     * Duplicate queries update the timestamp.
     * 
     * @param query The search query to save
     * @return Result indicating success or failure
     */
    suspend fun saveSearch(query: String): Result<Unit>
    
    /**
     * Clear all search history.
     * 
     * Removes all saved search queries from history.
     * 
     * @return Result indicating success or failure
     */
    suspend fun clearHistory(): Result<Unit>
    
    /**
     * Remove a specific search query from history.
     * 
     * @param query The search query to remove
     * @return Result indicating success or failure
     */
    suspend fun removeSearch(query: String): Result<Unit>
}
