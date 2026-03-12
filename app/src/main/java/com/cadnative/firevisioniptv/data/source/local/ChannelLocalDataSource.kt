package com.cadnative.firevisioniptv.data.source.local

import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for channel-related database operations.
 * 
 * This class wraps the ChannelDao and provides a clean interface for
 * accessing channel data from the local Room database. All operations
 * are executed on the IO dispatcher for optimal performance.
 * 
 * Requirements: TR-006 (Offline-first architecture with local database)
 */
@Singleton
class ChannelLocalDataSource @Inject constructor(
    private val channelDao: ChannelDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Get all active channels as a Flow.
     * 
     * @return Flow emitting list of all active channels ordered by name
     */
    fun getAllChannels(): Flow<List<ChannelEntity>> {
        return channelDao.getAllChannels()
    }
    
    /**
     * Get a specific channel by ID as a Flow.
     * 
     * @param channelId The unique identifier of the channel
     * @return Flow emitting the channel or null if not found
     */
    fun getChannelById(channelId: String): Flow<ChannelEntity?> {
        return channelDao.getChannelById(channelId)
    }
    
    /**
     * Get all channels in a specific category as a Flow.
     * 
     * @param categoryId The category identifier
     * @return Flow emitting list of channels in the category
     */
    fun getChannelsByCategory(categoryId: String): Flow<List<ChannelEntity>> {
        return channelDao.getChannelsByCategory(categoryId)
    }
    
    /**
     * Search channels by name or group title as a Flow.
     * 
     * @param query The search query
     * @return Flow emitting list of matching channels
     */
    fun searchChannels(query: String): Flow<List<ChannelEntity>> {
        return channelDao.searchChannels(query)
    }
    
    /**
     * Insert or replace multiple channels.
     * 
     * @param channels List of channels to insert
     */
    suspend fun insertChannels(channels: List<ChannelEntity>) = withContext(dispatcher) {
        channelDao.insertChannels(channels)
    }
    
    /**
     * Delete all channels from the database.
     */
    suspend fun deleteAllChannels() = withContext(dispatcher) {
        channelDao.deleteAllChannels()
    }
    
    /**
     * Replace all channels in the database with a new list.
     * 
     * This is a transactional operation that deletes all existing channels
     * and inserts the new ones atomically.
     * 
     * @param channels List of channels to replace with
     */
    suspend fun replaceAllChannels(channels: List<ChannelEntity>) = withContext(dispatcher) {
        channelDao.replaceAllChannels(channels)
    }
}
