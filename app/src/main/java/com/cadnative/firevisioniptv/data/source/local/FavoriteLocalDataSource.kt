package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for favorite-related database operations.
 * 
 * This class wraps the FavoriteDao and provides a clean interface for
 * accessing favorite data from the local Room database. All operations
 * are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: TR-006 (Offline-first architecture with local database)
 */
@Singleton
class FavoriteLocalDataSource @Inject constructor(
    private val favoriteDao: FavoriteDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get all favorite channels with full channel information as a Flow.
     * 
     * Performs a join between favorites and channels tables, returning
     * channels ordered by display order and then by date added.
     * 
     * @return Flow emitting list of favorite channels
     */
    fun getFavoriteChannels(): Flow<List<ChannelEntity>> {
        return favoriteDao.getFavoriteChannels()
    }
    
    /**
     * Check if a channel is marked as favorite as a Flow.
     * 
     * @param channelId The channel identifier to check
     * @return Flow emitting true if the channel is a favorite, false otherwise
     */
    fun isFavorite(channelId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(channelId)
    }
    
    /**
     * Get all favorite entities (without channel details) as a Flow.
     * 
     * @return Flow emitting list of favorite entities
     */
    fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }
    
    /**
     * Add a channel to favorites.
     * 
     * @param favorite The favorite entity to add
     */
    suspend fun addFavorite(favorite: FavoriteEntity) = withContext(dispatcher) {
        favoriteDao.addFavorite(favorite)
    }
    
    /**
     * Remove a channel from favorites.
     * 
     * @param channelId The channel identifier to remove
     */
    suspend fun removeFavorite(channelId: String) = withContext(dispatcher) {
        favoriteDao.removeFavorite(channelId)
    }
    
    /**
     * Update the display order of a favorite channel.
     * 
     * Used for reordering favorites in the UI.
     * 
     * @param channelId The channel identifier
     * @param order The new display order
     */
    suspend fun updateFavoriteOrder(channelId: String, order: Int) = withContext(dispatcher) {
        favoriteDao.updateFavoriteOrder(channelId, order)
    }
    
    /**
     * Delete all favorites from the database.
     */
    suspend fun deleteAllFavorites() = withContext(dispatcher) {
        favoriteDao.deleteAllFavorites()
    }
}
