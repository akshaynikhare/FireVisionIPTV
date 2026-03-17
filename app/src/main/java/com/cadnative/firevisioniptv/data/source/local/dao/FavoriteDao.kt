package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelEntity
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Favorite operations.
 * 
 * Provides join queries to retrieve favorite channels and operations
 * to manage the favorites list with ordering support.
 */
@Dao
interface FavoriteDao {
    
    /**
     * Get all favorite channels with full channel information.
     * 
     * Performs a join between favorites and channels tables, returning
     * channels ordered by display order and then by date added.
     * 
     * @return Flow emitting list of favorite channels
     */
    @Query("""
        SELECT c.* FROM channels c
        INNER JOIN favorites f ON c.id = f.channelId
        WHERE c.isActive = 1
        ORDER BY f.displayOrder ASC, f.addedAt DESC
    """)
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>
    
    /**
     * Check if a channel is marked as favorite.
     * 
     * @param channelId The channel identifier to check
     * @return Flow emitting true if the channel is a favorite, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    fun isFavorite(channelId: String): Flow<Boolean>
    
    /**
     * Add a channel to favorites.
     * 
     * @param favorite The favorite entity to add
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)
    
    /**
     * Remove a channel from favorites.
     * 
     * @param channelId The channel identifier to remove
     */
    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun removeFavorite(channelId: String)
    
    /**
     * Update the display order of a favorite channel.
     * 
     * Used for reordering favorites in the UI.
     * 
     * @param channelId The channel identifier
     * @param order The new display order
     */
    @Query("UPDATE favorites SET displayOrder = :order WHERE channelId = :channelId")
    suspend fun updateFavoriteOrder(channelId: String, order: Int)
    
    /**
     * Get all favorite entities (without channel details).
     * 
     * @return Flow emitting list of favorite entities
     */
    @Query("SELECT * FROM favorites ORDER BY displayOrder ASC, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    
    /**
     * Get all favorite channel IDs as a simple list.
     */
    @Query("SELECT channelId FROM favorites")
    suspend fun getFavoriteChannelIds(): List<String>

    /**
     * Delete all favorites from the database.
     */
    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()
}
