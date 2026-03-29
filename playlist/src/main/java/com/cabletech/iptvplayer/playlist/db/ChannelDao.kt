package com.cabletech.iptvplayer.playlist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE source_id = :sourceId ORDER BY sort_order ASC")
    fun observeBySource(sourceId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query(
        """SELECT * FROM channels
           WHERE (:group IS NULL OR group_title = :group)
           ORDER BY sort_order ASC"""
    )
    fun observeByGroup(group: String?): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT group_title FROM channels WHERE group_title IS NOT NULL ORDER BY group_title ASC")
    fun observeGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM channels WHERE source_id = :sourceId")
    suspend fun countBySource(sourceId: String): Int
}
