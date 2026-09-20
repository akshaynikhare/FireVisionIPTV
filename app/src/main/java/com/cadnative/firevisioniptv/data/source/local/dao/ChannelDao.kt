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
        val existing = getAllChannelIdentities()
        val idsToDelete = existing.map { it.id }.filter { it !in newIds }

        // Before the stale rows go, hand their user state to whichever incoming channel
        // is the same channel under a new id.
        remapRenamedChannels(channels, existing)

        idsToDelete.chunked(900).forEach { batch ->
            deleteChannelsByIds(batch)
        }
        insertChannels(channels)
    }

    /**
     * Carry favorites, health, metrics and resume points across a channel id change.
     *
     * A source can rename a channel between refreshes — a playlist reordered, an id
     * scheme changed, a provider reissuing its catalogue. The old row is then deleted
     * and the same channel reappears under a new id, which silently strips the install:
     * favorites stop joining to a channel, and health, metrics and resume points are
     * left keyed to ids nothing references. The user loses their list having done
     * nothing.
     *
     * So a channel is matched on what it actually is rather than what it is called:
     * first its stream URL, then its EPG id for a provider that rotated URLs. Both
     * passes require the key to identify exactly one row on each side — a playlist that
     * repeats a URL is ambiguous, and a wrong guess would move one channel's favorites
     * onto another. Only retired ids and genuinely new ids take part, so an unchanged
     * channel is never touched.
     */
    @Transaction
    suspend fun remapRenamedChannels(
        channels: List<ChannelEntity>,
        existing: List<ChannelIdentity>
    ) {
        if (existing.isEmpty() || channels.isEmpty()) return
        val incomingIds = channels.mapTo(HashSet()) { it.id }
        val existingIds = existing.mapTo(HashSet()) { it.id }

        val retired = existing.filter { it.id !in incomingIds }
        val arrived = channels.filter { it.id !in existingIds }
        if (retired.isEmpty() || arrived.isEmpty()) return

        val usedSources = HashSet<String>()
        val usedTargets = HashSet<String>()

        suspend fun pass(
            keyOfRetired: (ChannelIdentity) -> String?,
            keyOfArrived: (ChannelEntity) -> String?
        ) {
            val sources = retired.filterNot { it.id in usedSources }
                .groupBy(keyOfRetired)
                .filter { (key, rows) -> !key.isNullOrBlank() && rows.size == 1 }
            if (sources.isEmpty()) return
            val targets = arrived.filterNot { it.id in usedTargets }
                .groupBy(keyOfArrived)
                .filter { (key, rows) -> !key.isNullOrBlank() && rows.size == 1 }

            sources.forEach { (key, rows) ->
                val target = targets[key]?.first() ?: return@forEach
                val source = rows.first()
                remapChannelReferences(source.id, target.id)
                usedSources += source.id
                usedTargets += target.id
            }
        }

        pass({ it.streamUrl }, { it.streamUrl })
        pass({ it.tvgId }, { it.tvgId })
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

    @Query("SELECT id, streamUrl, tvgId FROM channels")
    suspend fun getAllChannelIdentities(): List<ChannelIdentity>

    @Query("SELECT * FROM channels WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelByIdSync(channelId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id IN (:ids) AND isActive = 1")
    suspend fun getChannelsByIds(ids: List<String>): List<ChannelEntity>

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteChannelsByIds(ids: List<String>)
}
