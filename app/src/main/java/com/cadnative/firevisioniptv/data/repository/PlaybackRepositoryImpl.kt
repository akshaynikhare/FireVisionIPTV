package com.cadnative.firevisioniptv.data.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.PlaybackLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.entity.PlaybackPositionEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.repository.PlaybackRepository
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
 * Implementation of PlaybackRepository with local database storage.
 * 
 * This repository manages playback positions with the following features:
 * 1. Local database as single source of truth
 * 2. Immediate local updates for responsive UI
 * 3. Automatic cleanup of old positions
 * 4. Graceful error handling
 * 
 * Requirements:
 * - TR-006: Network Performance (offline-first architecture)
 * - US-004.4: Playback position saved and restored
 */
@Singleton
class PlaybackRepositoryImpl @Inject constructor(
    private val localDataSource: PlaybackLocalDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : PlaybackRepository {
    
    /**
     * Get the playback position for a specific channel.
     * 
     * Emits local data immediately. The Flow will automatically emit
     * updated data when the position is saved.
     * 
     * @param channelId The channel identifier
     * @return Flow emitting Result with playback position in milliseconds, or null if not found
     */
    override fun getPlaybackPosition(channelId: String): Flow<Result<Long?>> = flow {
        localDataSource.getPosition(channelId)
            .map { entity -> entity?.position }
            .catch { e ->
                emit(Result.Error(Exception(e.message, e)))
            }
            .collect { position ->
                emit(Result.Success(position))
            }
    }.flowOn(dispatcher)
    
    /**
     * Save the playback position for a channel.
     * 
     * Updates local database immediately. Positions are stored with
     * the current timestamp for tracking and cleanup purposes.
     * 
     * @param channelId The channel identifier
     * @param position The playback position in milliseconds
     * @param duration The total duration in milliseconds
     * @return Result indicating success or failure
     */
    override suspend fun savePlaybackPosition(
        channelId: String,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(dispatcher) {
        try {
            // Validate inputs
            if (position < 0 || duration < 0) {
                return@withContext Result.Error(
                    IllegalArgumentException("Position and duration must be non-negative")
                )
            }
            
            if (position > duration) {
                return@withContext Result.Error(
                    IllegalArgumentException("Position cannot exceed duration")
                )
            }
            
            // Create entity and save to database
            val entity = PlaybackPositionEntity(
                channelId = channelId,
                position = position,
                duration = duration,
                lastPlayed = System.currentTimeMillis()
            )
            
            localDataSource.savePosition(entity)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Delete the playback position for a specific channel.
     * 
     * Removes the position from local database immediately.
     * 
     * @param channelId The channel identifier
     * @return Result indicating success or failure
     */
    override suspend fun deletePlaybackPosition(channelId: String): Result<Unit> = 
        withContext(dispatcher) {
            try {
                localDataSource.deletePosition(channelId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    
    /**
     * Get all playback positions ordered by last played (most recent first).
     * 
     * @return Flow emitting Result with map of channel IDs to positions
     */
    override fun getAllPlaybackPositions(): Flow<Result<Map<String, Long>>> = flow {
        localDataSource.getAllPositions()
            .map { entities ->
                entities.associate { entity ->
                    entity.channelId to entity.position
                }
            }
            .catch { e ->
                emit(Result.Error(Exception(e.message, e)))
            }
            .collect { positionsMap ->
                emit(Result.Success(positionsMap))
            }
    }.flowOn(dispatcher)
    
    /**
     * Clear old playback positions, keeping only the most recent ones.
     * 
     * This helps manage database size by removing positions for channels
     * that haven't been watched recently.
     * 
     * @param keepCount Number of recent positions to keep (default: 100)
     * @return Result indicating success or failure
     */
    override suspend fun clearOldPositions(keepCount: Int): Result<Unit> = 
        withContext(dispatcher) {
            try {
                if (keepCount < 0) {
                    return@withContext Result.Error(
                        IllegalArgumentException("keepCount must be non-negative")
                    )
                }
                
                localDataSource.deleteOldPositions(keepCount)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
}
