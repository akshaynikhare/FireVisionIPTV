package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.StreamMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamMetricsDao {

    @Query("SELECT * FROM stream_metrics WHERE channelId = :channelId")
    suspend fun getByChannelId(channelId: String): StreamMetricsEntity?

    @Query("SELECT * FROM stream_metrics")
    fun observeAll(): Flow<List<StreamMetricsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StreamMetricsEntity)

    @Query("""
        UPDATE stream_metrics
        SET playCount = playCount + 1,
            aliveCount = aliveCount + 1,
            lastPlayedAt = :timestamp,
            lastAliveAt = :timestamp
        WHERE channelId = :channelId
    """)
    suspend fun incrementPlay(channelId: String, timestamp: Long)

    @Query("""
        UPDATE stream_metrics
        SET deadCount = deadCount + 1,
            lastDeadAt = :timestamp
        WHERE channelId = :channelId
    """)
    suspend fun incrementDead(channelId: String, timestamp: Long)

    @Query("""
        UPDATE stream_metrics
        SET aliveCount = aliveCount + 1,
            lastAliveAt = :timestamp
        WHERE channelId = :channelId
    """)
    suspend fun incrementAlive(channelId: String, timestamp: Long)

    @Query("""
        UPDATE stream_metrics
        SET unresponsiveCount = unresponsiveCount + 1,
            lastUnresponsiveAt = :timestamp
        WHERE channelId = :channelId
    """)
    suspend fun incrementUnresponsive(channelId: String, timestamp: Long)

    @Query("DELETE FROM stream_metrics")
    suspend fun deleteAll()

    @Query("DELETE FROM stream_metrics WHERE channelId NOT IN (SELECT id FROM channels)")
    suspend fun cleanupOrphaned()
}
