package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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
     * Insert or update multiple channels.
     *
     * Uses @Upsert instead of @Insert(REPLACE) to avoid triggering
     * ON DELETE CASCADE on foreign keys (favorites, channel_health).
     * REPLACE internally does DELETE+INSERT which cascades and wipes favorites.
     *
     * @param channels List of channels to upsert
     */
    @Upsert
    suspend fun insertChannels(channels: List<ChannelEntity>)
    
    /**
     * Delete all channels from the database.
     */
    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
    
    /**
     * Replace all channels in the database with a new list.
     *
     * Uses upsert + selective delete to preserve foreign key relationships
     * (favorites, channel_health) for channels that still exist.
     *
     * @param channels List of channels to replace with
     */
    @Transaction
    suspend fun replaceAllChannels(channels: List<ChannelEntity>) {
        val newIds = channels.map { it.id }.toSet()
        val existingIds = getAllChannelIds()
        val idsToDelete = existingIds.filter { it !in newIds }

        idsToDelete.chunked(900).forEach { batch ->
            deleteChannelsByIds(batch)
        }
        insertChannels(channels)
    }

    /**
     * Replace all channels, first re-pointing per-channel user state from ids a source
     * no longer emits to the ids it emits now.
     *
     * Without this, changing a source's id scheme silently strips the install: the old
     * rows are deleted below, so favorites stop joining to a channel and health, metrics
     * and resume points are left keyed to ids nothing references. Runs in one transaction
     * with the replace so a crash mid-way cannot leave state half-migrated.
     *
     * @param channels List of channels to replace with
     * @param legacyIdAliases Map of retired channel id to the id that now supersedes it
     */
    @Transaction
    suspend fun replaceAllChannels(
        channels: List<ChannelEntity>,
        legacyIdAliases: Map<String, String>
    ) {
        if (legacyIdAliases.isNotEmpty()) {
            val existingIds = getAllChannelIds().toSet()
            val incomingIds = channels.mapTo(HashSet()) { it.id }
            legacyIdAliases.forEach { (legacyId, newId) ->
                // Only migrate a legacy row this install actually holds, and only onto an
                // id the incoming list really defines — never invent a dangling reference.
                if (legacyId != newId && legacyId in existingIds && newId in incomingIds) {
                    remapChannelReferences(legacyId, newId)
                }
            }
        }
        replaceAllChannels(channels)
    }

    /**
     * Move every per-channel user record from [legacyId] to [newId].
     *
     * `UPDATE OR IGNORE` because each of these tables is unique on channelId: if the
     * new id already carries a record, the existing one wins and the legacy row is left
     * to be cleaned up as an ordinary orphan rather than failing the whole refresh.
     */
    @Transaction
    suspend fun remapChannelReferences(legacyId: String, newId: String) {
        remapFavoriteChannelId(legacyId, newId)
        remapChannelHealthChannelId(legacyId, newId)
        remapStreamMetricsChannelId(legacyId, newId)
        remapPlaybackPositionChannelId(legacyId, newId)
    }

    @Query("UPDATE OR IGNORE favorites SET channelId = :newId WHERE channelId = :legacyId")
    suspend fun remapFavoriteChannelId(legacyId: String, newId: String)

    @Query("UPDATE OR IGNORE channel_health SET channelId = :newId WHERE channelId = :legacyId")
    suspend fun remapChannelHealthChannelId(legacyId: String, newId: String)

    @Query("UPDATE OR IGNORE stream_metrics SET channelId = :newId WHERE channelId = :legacyId")
    suspend fun remapStreamMetricsChannelId(legacyId: String, newId: String)

    @Query("UPDATE OR IGNORE playback_positions SET channelId = :newId WHERE channelId = :legacyId")
    suspend fun remapPlaybackPositionChannelId(legacyId: String, newId: String)

    @Query("SELECT id FROM channels")
    suspend fun getAllChannelIds(): List<String>

    @Query("SELECT * FROM channels WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelByIdSync(channelId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id IN (:ids) AND isActive = 1")
    suspend fun getChannelsByIds(ids: List<String>): List<ChannelEntity>

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteChannelsByIds(ids: List<String>)
}
