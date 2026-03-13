package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for playback position-related database operations.
 * 
 * This class wraps the PlaybackPositionDao and provides a clean interface for
 * the repository layer to interact with the local database. All database
 * operations are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: TR-006 (Network Performance - Offline-first architecture)
 */
@Singleton
class PlaybackLocalDataSource @Inject constructor(
    private val playbackPositionDao: PlaybackPositionDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     * @return Flow emitting the playback position or null if not found
     */
    fun getPosition(channelId: String): Flow<PlaybackPositionEntity?> {
        return playbackPositionDao.getPosition(channelId)
    }
    
    /**
     * Get all playback positions ordered by last played (most recent first).
     * 
     * @return Flow emitting list of all playback positions
     */
    fun getAllPositions(): Flow<List<PlaybackPositionEntity>> {
        return playbackPositionDao.getAllPositions()
    }
    
    /**
     * Save or update a playback position for a channel.
     * 
     * @param position The playback position to save
     */
    suspend fun savePosition(position: PlaybackPositionEntity) = withContext(dispatcher) {
        playbackPositionDao.savePosition(position)
    }
    
    /**
     * Delete the playback position for a specific channel.
     * 
     * @param channelId The channel identifier
     */
    suspend fun deletePosition(channelId: String) = withContext(dispatcher) {
        playbackPositionDao.deletePosition(channelId)
    }
    
    /**
     * Delete all playback positions.
     */
    suspend fun deleteAllPositions() = withContext(dispatcher) {
        playbackPositionDao.deleteAllPositions()
    }
    
    /**
     * Delete old playback positions, keeping only the most recent ones.
     * 
     * @param keepCount Number of recent positions to keep
     */
    suspend fun deleteOldPositions(keepCount: Int = 100) = withContext(dispatcher) {
        playbackPositionDao.deleteOldPositions(keepCount)
    }
}
