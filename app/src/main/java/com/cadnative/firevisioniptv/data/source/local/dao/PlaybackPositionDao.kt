package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Playback Position operations.
 * 
 * Provides operations to save and retrieve playback positions for channels.
 */
@Dao
interface PlaybackPositionDao {
    
    /**
     * Get the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     * @return Flow emitting the playback position or null if not found
     */
    @Query("SELECT * FROM playback_positions WHERE channelId = :channelId")
    fun getPosition(channelId: String): Flow<PlaybackPositionEntity?>
    
    /**
     * Get all playback positions ordered by last played (most recent first).
     * 
     * @return Flow emitting list of all playback positions
     */
    @Query("SELECT * FROM playback_positions ORDER BY lastPlayed DESC")
    fun getAllPositions(): Flow<List<PlaybackPositionEntity>>
    
    /**
     * Save or update a playback position for a channel.
     * 
     * @param position The playback position to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(position: PlaybackPositionEntity)
    
    /**
     * Delete the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     */
    @Query("DELETE FROM playback_positions WHERE channelId = :channelId")
    suspend fun deletePosition(channelId: String)
    
    /**
     * Delete all playback positions.
     */
    @Query("DELETE FROM playback_positions")
    suspend fun deleteAllPositions()
    
    /**
     * Get channel IDs of recently watched channels (one-shot).
     */
    @Query("SELECT channelId FROM playback_positions ORDER BY lastPlayed DESC LIMIT :limit")
    suspend fun getRecentlyWatchedIds(limit: Int = 20): List<String>

    /**
     * Observe channel IDs of recently watched channels (reactive).
     * Re-emits whenever playback_positions table changes.
     */
    @Query("SELECT channelId FROM playback_positions ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecentlyWatchedIds(limit: Int = 20): Flow<List<String>>

    /**
     * Get popular category IDs based on recent plays (one-shot).
     */
    @Query("""
        SELECT c.categoryId FROM playback_positions p
        INNER JOIN channels c ON p.channelId = c.id
        WHERE c.isActive = 1
        GROUP BY c.categoryId
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """)
    suspend fun getPopularCategoryIds(limit: Int = 10): List<String>

    /**
     * Observe popular category IDs based on recent plays (reactive).
     */
    @Query("""
        SELECT c.categoryId FROM playback_positions p
        INNER JOIN channels c ON p.channelId = c.id
        WHERE c.isActive = 1
        GROUP BY c.categoryId
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """)
    fun observePopularCategoryIds(limit: Int = 10): Flow<List<String>>

    /**
     * Delete old playback positions, keeping only the most recent ones.
     * 
     * @param keepCount Number of recent positions to keep
     */
    @Query("""
        DELETE FROM playback_positions 
        WHERE channelId NOT IN (
            SELECT channelId FROM playback_positions 
            ORDER BY lastPlayed DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldPositions(keepCount: Int = 100)
}
