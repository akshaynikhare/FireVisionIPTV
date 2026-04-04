package com.cadnative.firevisioniptv.data.repository

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.FavoritesRequest
import com.cadnative.firevisioniptv.data.source.local.FavoriteLocalDataSource
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.FavoriteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FavoriteRepository with offline-first strategy and background sync.
 * 
 * This repository manages favorite channels with the following features:
 * 1. Local database as single source of truth
 * 2. Immediate local updates for responsive UI
 * 3. Background synchronization with server
 * 4. Conflict resolution (local changes take precedence)
 * 5. Graceful error handling
 * 
 * Requirements:
 * - TR-006: Network Performance (offline-first architecture, background sync)
 * - US-008: Enhanced Favorites (sync across devices, quick add/remove)
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val localDataSource: FavoriteLocalDataSource,
    private val apiService: FireVisionApiService,
    private val channelMapper: ChannelMapper,
    private val channelDao: ChannelDao,
    private val application: Application,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : FavoriteRepository, Closeable {

    private val syncScope = CoroutineScope(SupervisorJob() + dispatcher)

    companion object {
        private const val TAG = "FavoriteRepo"
    }
    
    /**
     * Get all favorite channels with offline-first strategy.
     * 
     * Emits local data immediately. The Flow will automatically emit
     * updated data when favorites are added/removed.
     */
    override fun getFavoriteChannels(): Flow<Result<List<Channel>>> = flow {
        localDataSource.getFavoriteChannels()
            .map { entities ->
                entities.map { entity ->
                    channelMapper.toDomain(entity, isFavorite = true)
                }
            }
            .catch { e ->
                emit(Result.Error(Exception(e.message, e)))
            }
            .collect { channels ->
                emit(Result.Success(channels))
            }
    }.flowOn(dispatcher)
    
    /**
     * Check if a channel is marked as favorite.
     * 
     * @param channelId The channel ID to check
     * @return Flow emitting Result with boolean indicating favorite status
     */
    override fun isFavorite(channelId: String): Flow<Result<Boolean>> = flow {
        localDataSource.isFavorite(channelId)
            .catch { e ->
                emit(Result.Error(Exception(e.message, e)))
            }
            .collect { isFavorite ->
                emit(Result.Success(isFavorite))
            }
    }.flowOn(dispatcher)
    
    /**
     * Add a channel to favorites.
     * 
     * Updates local database immediately and triggers background sync.
     * 
     * @param channelId The ID of the channel to add
     * @return Result indicating success or failure
     */
    override suspend fun addFavorite(channelId: String): Result<Unit> = withContext(dispatcher) {
        try {
            // Check if already a favorite
            val isAlreadyFavorite = localDataSource.isFavorite(channelId).first()
            if (isAlreadyFavorite) {
                return@withContext Result.Success(Unit)
            }
            
            // Add to local database
            val favorite = FavoriteEntity(
                channelId = channelId,
                addedAt = System.currentTimeMillis(),
                displayOrder = 0
            )
            localDataSource.addFavorite(favorite)
            
            // Trigger background sync (fire and forget)
            syncFavoritesInBackground()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Remove a channel from favorites.
     * 
     * Updates local database immediately and triggers background sync.
     * 
     * @param channelId The ID of the channel to remove
     * @return Result indicating success or failure
     */
    override suspend fun removeFavorite(channelId: String): Result<Unit> = withContext(dispatcher) {
        try {
            // Remove from local database
            localDataSource.removeFavorite(channelId)
            
            // Trigger background sync (fire and forget)
            syncFavoritesInBackground()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Toggle favorite status for a channel.
     * 
     * If the channel is currently a favorite, it will be removed.
     * If it's not a favorite, it will be added.
     * 
     * @param channelId The ID of the channel to toggle
     * @return Result indicating success or failure
     */
    override suspend fun toggleFavorite(channelId: String): Result<Unit> = withContext(dispatcher) {
        try {
            val isFavorite = localDataSource.isFavorite(channelId).first()
            
            if (isFavorite) {
                removeFavorite(channelId)
            } else {
                addFavorite(channelId)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Reorder favorites by updating display order.
     * 
     * @param channelId The ID of the channel to reorder
     * @param newOrder The new display order position
     * @return Result indicating success or failure
     */
    override suspend fun updateFavoriteOrder(channelId: String, newOrder: Int): Result<Unit> = 
        withContext(dispatcher) {
            try {
                localDataSource.updateFavoriteOrder(channelId, newOrder)
                
                // Trigger background sync (fire and forget)
                syncFavoritesInBackground()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    
    /**
     * Synchronize favorites with the server.
     * 
     * This uploads local favorite changes to the server. Local changes
     * take precedence in case of conflicts (last-write-wins strategy).
     * 
     * @return Result indicating success or failure of synchronization
     */
    override suspend fun syncFavorites(): Result<Unit> = withContext(dispatcher) {
        try {
            // Get all local favorites
            val favorites = localDataSource.getAllFavorites().first()
            val channelIds = favorites.map { it.channelId }
            
            // Prepare sync request
            val request = FavoritesRequest(
                channelIds = channelIds,
                deviceId = getDeviceId(),
                timestamp = System.currentTimeMillis()
            )
            
            // Send to server
            val response = apiService.syncFavorites(request)
            
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Sync failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            // Sync failures are non-critical - local data is still valid
            Result.Error(e)
        }
    }
    
    private fun isPaired(): Boolean {
        val tvCode = AppPreferences.getTvCode(application)
        return tvCode.isNotEmpty() && tvCode != AppPreferences.DEFAULT_TV_CODE
    }

    /**
     * Trigger background sync without blocking the caller.
     * Skips sync for unpaired (default playlist) users — favorites are local only.
     */
    private fun syncFavoritesInBackground() {
        if (!isPaired()) return
        syncScope.launch {
            try {
                syncFavorites()
            } catch (e: Exception) {
                Log.w(TAG, "Background favorites sync failed: ${e.message}")
            }
        }
    }
    
    override suspend fun pullFavoritesFromServer(): Result<Unit> = withContext(dispatcher) {
        if (!isPaired()) return@withContext Result.Success(Unit)
        try {
            val response = apiService.getFavorites()
            if (response.isSuccessful) {
                val serverChannelIds = response.body()?.channelIds ?: emptyList()
                val localFavorites = localDataSource.getAllFavorites().first()
                val localChannelIds = localFavorites.map { it.channelId }.toSet()

                // Add server favorites that don't exist locally
                // Skip channelIds not in local channels table (FK constraint)
                for (channelId in serverChannelIds) {
                    if (channelId !in localChannelIds) {
                        val channelExists = channelDao.getChannelByIdSync(channelId) != null
                        if (channelExists) {
                            val favorite = FavoriteEntity(
                                channelId = channelId,
                                addedAt = System.currentTimeMillis(),
                                displayOrder = 0
                            )
                            localDataSource.addFavorite(favorite)
                        }
                    }
                }

                Result.Success(Unit)
            } else {
                Result.Error(Exception("Pull favorites failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pull favorites from server failed: ${e.message}")
            Result.Error(e)
        }
    }

    override fun close() {
        syncScope.cancel()
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}
