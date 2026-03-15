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
 * the repository layer to interact with the local database. All database
 * operations are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: TR-006 (Network Performance - Offline-first architecture)
 */
@Singleton
class FavoriteLocalDataSource @Inject constructor(
    private val favoriteDao: FavoriteDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get all favorite channels with full channel information.
     * 
     * @return Flow emitting list of favorite channels
     */
    fun getFavoriteChannels(): Flow<List<ChannelEntity>> {
        return favoriteDao.getFavoriteChannels()
    }
    
    /**
     * Check if a channel is marked as favorite.
     * 
     * @param channelId The channel identifier to check
     * @return Flow emitting true if the channel is a favorite, false otherwise
     */
    fun isFavorite(channelId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(channelId)
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
     * @param channelId The channel identifier
     * @param order The new display order
     */
    suspend fun updateFavoriteOrder(channelId: String, order: Int) = withContext(dispatcher) {
        favoriteDao.updateFavoriteOrder(channelId, order)
    }
    
    /**
     * Get all favorite entities (without channel details).
     * 
     * @return Flow emitting list of favorite entities
     */
    fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }
    
    /**
     * Delete all favorites from the database.
     */
    suspend fun deleteAllFavorites() = withContext(dispatcher) {
        favoriteDao.deleteAllFavorites()
    }
}
