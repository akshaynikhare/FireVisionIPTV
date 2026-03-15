package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for playback position operations.
 * 
 * This interface defines the contract for managing playback positions,
 * allowing users to resume watching from where they left off.
 * 
 * Requirements: US-004.4 (Playback position saved and restored)
 */
interface PlaybackRepository {
    
    /**
     * Get the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     * @return Flow emitting Result with playback position in milliseconds, or null if not found
     */
    fun getPlaybackPosition(channelId: String): Flow<Result<Long?>>
    
    /**
     * Save the playback position for a channel.
     * 
     * @param channelId The channel identifier
     * @param position The playback position in milliseconds
     * @param duration The total duration in milliseconds
     * @return Result indicating success or failure
     */
    suspend fun savePlaybackPosition(
        channelId: String,
        position: Long,
        duration: Long
    ): Result<Unit>
    
    /**
     * Delete the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     * @return Result indicating success or failure
     */
    suspend fun deletePlaybackPosition(channelId: String): Result<Unit>
    
    /**
     * Get all playback positions ordered by last played (most recent first).
     * 
     * @return Flow emitting Result with map of channel IDs to positions
     */
    fun getAllPlaybackPositions(): Flow<Result<Map<String, Long>>>
    
    /**
     * Clear old playback positions, keeping only the most recent ones.
     * 
     * @param keepCount Number of recent positions to keep (default: 100)
     * @return Result indicating success or failure
     */
    suspend fun clearOldPositions(keepCount: Int = 100): Result<Unit>
}
