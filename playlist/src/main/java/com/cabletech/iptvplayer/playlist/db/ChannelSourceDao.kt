package com.cabletech.iptvplayer.playlist.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelSourceDao {

    @Query("SELECT * FROM channel_sources ORDER BY name ASC")
    fun observeAll(): Flow<List<ChannelSourceEntity>>

    @Query("SELECT * FROM channel_sources WHERE id = :id")
    suspend fun getById(id: String): ChannelSourceEntity?

    @Query("SELECT * FROM channel_sources WHERE is_enabled = 1 ORDER BY name ASC")
    suspend fun getEnabled(): List<ChannelSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: ChannelSourceEntity)

    @Update
    suspend fun update(source: ChannelSourceEntity)

    @Delete
    suspend fun delete(source: ChannelSourceEntity)

    @Query("UPDATE channel_sources SET last_refreshed_at = :timestamp, channel_count = :count WHERE id = :id")
    suspend fun markRefreshed(id: String, timestamp: Long, count: Int)
}
