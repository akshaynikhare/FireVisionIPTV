package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for favorite channel operations.
 * 
 * This interface defines the contract for managing favorite channels,
 * including local storage and server synchronization.
 */
interface FavoriteRepository {
    
    /**
     * Get all favorite channels.
     * 
     * @return Flow emitting Result with list of favorite channels
     */
    fun getFavoriteChannels(): Flow<Result<List<Channel>>>
    
    /**
     * Check if a channel is marked as favorite.
     * 
     * @param channelId The channel ID to check
     * @return Flow emitting Result with boolean indicating favorite status
     */
    fun isFavorite(channelId: String): Flow<Result<Boolean>>
    
    /**
     * Add a channel to favorites.
     * 
     * @param channelId The ID of the channel to add
     * @return Result indicating success or failure
     */
    suspend fun addFavorite(channelId: String): Result<Unit>
    
    /**
     * Remove a channel from favorites.
     * 
     * @param channelId The ID of the channel to remove
     * @return Result indicating success or failure
     */
    suspend fun removeFavorite(channelId: String): Result<Unit>
    
    /**
     * Toggle favorite status for a channel.
     * 
     * If the channel is currently a favorite, it will be removed.
     * If it's not a favorite, it will be added.
     * 
     * @param channelId The ID of the channel to toggle
     * @return Result indicating success or failure
     */
    suspend fun toggleFavorite(channelId: String): Result<Unit>
    
    /**
     * Reorder favorites by updating display order.
     * 
     * @param channelId The ID of the channel to reorder
     * @param newOrder The new display order position
     * @return Result indicating success or failure
     */
    suspend fun updateFavoriteOrder(channelId: String, newOrder: Int): Result<Unit>
    
    /**
     * Synchronize favorites with the server.
     * 
     * This uploads local favorite changes to the server and downloads
     * any server-side changes.
     * 
     * @return Result indicating success or failure of synchronization
     */
    suspend fun syncFavorites(): Result<Unit>
}
