package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelHealthDao {

    @Query("SELECT * FROM channel_health")
    fun getAllHealth(): Flow<List<ChannelHealthEntity>>

    @Query("SELECT * FROM channel_health WHERE channelId = :channelId")
    fun getHealthByChannelId(channelId: String): Flow<ChannelHealthEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(health: ChannelHealthEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(health: List<ChannelHealthEntity>)

    /**
     * Upsert health fields while preserving the existing thumbnailPath.
     * Uses a subquery to carry forward the thumbnail before REPLACE deletes the row.
     */
    @Query("""
        INSERT OR REPLACE INTO channel_health
            (channelId, status, lastCheckedAt, responseTimeMs, errorMessage, thumbnailPath)
        VALUES (
            :channelId, :status, :lastCheckedAt, :responseTimeMs, :errorMessage,
            (SELECT thumbnailPath FROM channel_health WHERE channelId = :channelId)
        )
    """)
    suspend fun upsertPreservingThumbnail(
        channelId: String,
        status: String,
        lastCheckedAt: Long,
        responseTimeMs: Long?,
        errorMessage: String?
    )

    @Query("""
        SELECT c.id FROM channels c
        LEFT JOIN channel_health h ON c.id = h.channelId
        WHERE c.isActive = 1
        ORDER BY COALESCE(h.lastCheckedAt, 0) ASC
        LIMIT :limit
    """)
    suspend fun getStaleChannelIds(limit: Int): List<String>

    @Query("""
        SELECT c.id FROM channels c
        LEFT JOIN channel_health h ON c.id = h.channelId
        WHERE c.isActive = 1
        ORDER BY COALESCE(h.lastCheckedAt, 0) ASC
    """)
    suspend fun getAllChannelIdsByPriority(): List<String>

    @Query("SELECT channelId FROM channel_health WHERE status = 'ONLINE' AND thumbnailPath IS NULL")
    suspend fun getOnlineChannelIdsWithoutThumbnail(): List<String>

    @Query("UPDATE channel_health SET thumbnailPath = :path WHERE channelId = :channelId")
    suspend fun updateThumbnailPath(channelId: String, path: String)

    @Query("UPDATE channel_health SET thumbnailPath = NULL")
    suspend fun clearAllThumbnailPaths()

    @Query("SELECT COUNT(*) FROM channels WHERE isActive = 1")
    suspend fun getTotalActiveChannelCount(): Int

    @Query("SELECT COUNT(*) FROM channel_health WHERE status != 'UNKNOWN'")
    suspend fun getScannedChannelCount(): Int

    @Query("DELETE FROM channel_health")
    suspend fun deleteAll()

    @Query("DELETE FROM channel_health WHERE channelId NOT IN (SELECT id FROM channels)")
    suspend fun cleanupOrphaned()
}
