package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for channel data operations.
 * 
 * This interface defines the contract for accessing and managing channel data,
 * following the Repository pattern from Clean Architecture. The data layer
 * will provide the implementation with offline-first strategy.
 * 
 * All Flow-based methods emit updates when the underlying data changes,
 * enabling reactive UI updates. Suspend methods are for one-time operations.
 */
interface ChannelRepository {
    
    /**
     * Get all channels as a Flow that emits updates when data changes.
     * 
     * @return Flow emitting Result with list of channels
     */
    fun getChannels(): Flow<Result<List<Channel>>>
    
    /**
     * Get a specific channel by ID.
     * 
     * @param id The channel ID
     * @return Flow emitting Result with the channel or error if not found
     */
    fun getChannelById(id: String): Flow<Result<Channel>>
    
    /**
     * Get all channels in a specific category.
     * 
     * @param category The category name
     * @return Flow emitting Result with list of channels in the category
     */
    fun getChannelsByCategory(category: String): Flow<Result<List<Channel>>>
    
    /**
     * Search channels by query string.
     * 
     * @param query The search query
     * @return Flow emitting Result with list of matching channels
     */
    fun searchChannels(query: String): Flow<Result<List<Channel>>>
    
    /**
     * Refresh channels from remote source.
     * 
     * This triggers a fetch from the remote API and updates the local cache.
     * The Flow-based methods will automatically emit the updated data.
     * 
     * @return Result indicating success or failure of the refresh operation
     */
    suspend fun refreshChannels(): Result<Unit>
    
    /**
     * Add a channel to favorites.
     * 
     * @param channelId The ID of the channel to add to favorites
     * @return Result indicating success or failure
     */
    suspend fun addToFavorites(channelId: String): Result<Unit>
    
    /**
     * Remove a channel from favorites.
     * 
     * @param channelId The ID of the channel to remove from favorites
     * @return Result indicating success or failure
     */
    suspend fun removeFromFavorites(channelId: String): Result<Unit>
    
    /**
     * Get all favorite channels.
     * 
     * @return Flow emitting Result with list of favorite channels
     */
    fun getFavoriteChannels(): Flow<Result<List<Channel>>>
}
