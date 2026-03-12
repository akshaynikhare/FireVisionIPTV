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
