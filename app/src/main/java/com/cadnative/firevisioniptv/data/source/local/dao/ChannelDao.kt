package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Channel operations.
 * 
 * Provides Flow-based reactive queries for channel data with support for
 * filtering, searching, and batch operations.
 */
@Dao
interface ChannelDao {
    
    /**
     * Get all active channels ordered by name.
     * 
     * @return Flow emitting list of all active channels
     */
    @Query("SELECT * FROM channels WHERE isActive = 1 ORDER BY name ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>
    
    /**
     * Get a specific channel by ID.
     * 
     * @param channelId The unique identifier of the channel
     * @return Flow emitting the channel or null if not found
     */
    @Query("SELECT * FROM channels WHERE id = :channelId")
    fun getChannelById(channelId: String): Flow<ChannelEntity?>
    
    /**
     * Get all channels in a specific category.
     * 
     * @param categoryId The category identifier
     * @return Flow emitting list of channels in the category
     */
    @Query("SELECT * FROM channels WHERE categoryId = :categoryId AND isActive = 1 ORDER BY name ASC")
    fun getChannelsByCategory(categoryId: String): Flow<List<ChannelEntity>>
    
    /**
     * Search channels by name or group title.
     * 
     * Performs case-insensitive search on channel name and group title fields.
     * 
     * @param query The search query
     * @return Flow emitting list of matching channels
     */
    @Query("""
        SELECT * FROM channels 
        WHERE (name LIKE '%' || :query || '%' 
           OR groupTitle LIKE '%' || :query || '%')
           AND isActive = 1
        ORDER BY name ASC
    """)
    fun searchChannels(query: String): Flow<List<ChannelEntity>>
    
    /**
     * Insert or replace multiple channels.
     * 
     * @param channels List of channels to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)
    
    /**
     * Delete all channels from the database.
     */
    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
    
    /**
     * Replace all channels in the database with a new list.
     * 
     * This is a transactional operation that deletes all existing channels
     * and inserts the new ones atomically.
     * 
     * @param channels List of channels to replace with
     */
    @Transaction
    suspend fun replaceAllChannels(channels: List<ChannelEntity>) {
        deleteAllChannels()
        insertChannels(channels)
    }
}
