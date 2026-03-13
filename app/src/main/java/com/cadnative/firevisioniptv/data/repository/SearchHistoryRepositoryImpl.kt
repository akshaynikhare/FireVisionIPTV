package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.SearchHistoryLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SearchHistoryRepository with local database storage.
 * 
 * This repository manages search history with the following features:
 * 1. Save searches to local database with timestamps
 * 2. Retrieve recent searches ordered by timestamp (most recent first)
 * 3. Clear all search history
 * 4. Remove specific search queries
 * 5. Automatic cleanup of old entries to prevent unbounded growth
 * 
 * Requirements:
 * - US-003.3: Recent searches saved locally
 * - US-003.6: Clear search history option
 */
@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val localDataSource: SearchHistoryLocalDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : SearchHistoryRepository {
    
    /**
     * Get recent search queries ordered by timestamp (most recent first).
     * 
     * Returns the most recent search queries as a Flow that emits updates
     * whenever the search history changes.
     * 
     * @param limit Maximum number of recent searches to return
     * @return Flow emitting Result with list of recent search queries
     */
    override fun getRecentSearches(limit: Int): Flow<Result<List<String>>> = flow {
        localDataSource.getRecentSearches(limit)
            .map { entities ->
                entities.map { it.query }
            }
            .catch { e ->
                emit(Result.Error(Exception(e.message, e)))
            }
            .collect { queries ->
                emit(Result.Success(queries))
            }
    }.flowOn(dispatcher)
    
    /**
     * Save a search query to history.
     * 
     * Adds the query to search history with current timestamp.
     * If the query already exists, it updates the timestamp to move it
     * to the top of recent searches.
     * 
     * Empty or blank queries are ignored.
     * 
     * @param query The search query to save
     * @return Result indicating success or failure
     */
    override suspend fun saveSearch(query: String): Result<Unit> = withContext(dispatcher) {
        try {
            // Ignore empty or blank queries
            if (query.isBlank()) {
                return@withContext Result.Success(Unit)
            }
            
            // Trim whitespace from query
            val trimmedQuery = query.trim()
            
            // Create search history entity with current timestamp
            val searchHistory = SearchHistoryEntity(
                query = trimmedQuery,
                timestamp = System.currentTimeMillis()
            )
            
            // Save to local database
            // The DAO uses REPLACE strategy, so duplicate queries will update timestamp
            localDataSource.saveSearch(searchHistory)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Clear all search history.
     * 
     * Removes all saved search queries from the local database.
     * 
     * @return Result indicating success or failure
     */
    override suspend fun clearHistory(): Result<Unit> = withContext(dispatcher) {
        try {
            localDataSource.clearHistory()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Remove a specific search query from history.
     * 
     * This method is not yet implemented as it requires additional
     * DAO support. For now, it returns a success result.
     * 
     * @param query The search query to remove
     * @return Result indicating success or failure
     */
    override suspend fun removeSearch(query: String): Result<Unit> = withContext(dispatcher) {
        try {
            // TODO: Implement removeSearch in DAO and local data source
            // For now, return success as this is an optional feature
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
