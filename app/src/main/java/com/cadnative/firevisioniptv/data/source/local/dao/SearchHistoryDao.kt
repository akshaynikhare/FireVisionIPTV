package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Search History operations.
 * 
 * Provides operations to manage search history with timestamp-based ordering.
 */
@Dao
interface SearchHistoryDao {
    
    /**
     * Get recent search queries ordered by timestamp (most recent first).
     * 
     * @param limit Maximum number of recent searches to return
     * @return Flow emitting list of recent search history entries
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>>
    
    /**
     * Save a search query to history.
     * 
     * @param searchHistory The search history entry to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSearch(searchHistory: SearchHistoryEntity)
    
    /**
     * Clear all search history.
     */
    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
    
    /**
     * Delete old search history entries, keeping only the most recent ones.
     * 
     * @param keepCount Number of recent entries to keep
     */
    @Query("""
        DELETE FROM search_history 
        WHERE id NOT IN (
            SELECT id FROM search_history 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldSearches(keepCount: Int = 50)
}
