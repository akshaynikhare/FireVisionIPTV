package com.cabletech.iptvplayer.epg.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cabletech.iptvplayer.epg.EpgProgramme
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {

    // --- Sources ---

    @Upsert
    suspend fun upsertSource(source: EpgSourceEntity)

    @Query("SELECT * FROM epg_sources ORDER BY name ASC")
    fun observeSources(): Flow<List<EpgSourceEntity>>

    @Query("SELECT * FROM epg_sources WHERE is_enabled = 1")
    suspend fun getEnabledSources(): List<EpgSourceEntity>

    @Query("UPDATE epg_sources SET last_refreshed_at = :ts WHERE id = :id")
    suspend fun markSourceRefreshed(id: String, ts: Long)

    // --- Programmes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<EpgProgrammeEntity>)

    @Query("DELETE FROM epg_programmes WHERE channel_id IN (SELECT DISTINCT channel_id FROM epg_programmes LIMIT -1)")
    suspend fun deleteAll()

    /**
     * Returns the programme currently airing on [channelId] at [nowMs], or null.
     */
    @Query(
        """SELECT * FROM epg_programmes
           WHERE channel_id = :channelId
             AND start_utc_ms <= :nowMs
             AND stop_utc_ms  >  :nowMs
           LIMIT 1"""
    )
    suspend fun getNowPlaying(channelId: String, nowMs: Long): EpgProgrammeEntity?

    /**
     * Returns the next programme airing on [channelId] after [nowMs].
     */
    @Query(
        """SELECT * FROM epg_programmes
           WHERE channel_id = :channelId
             AND start_utc_ms > :nowMs
           ORDER BY start_utc_ms ASC
           LIMIT 1"""
    )
    suspend fun getNextUp(channelId: String, nowMs: Long): EpgProgrammeEntity?

    /**
     * Returns the full day's schedule for [channelId] within a time window.
     */
    @Query(
        """SELECT * FROM epg_programmes
           WHERE channel_id = :channelId
             AND stop_utc_ms  > :fromMs
             AND start_utc_ms < :toMs
           ORDER BY start_utc_ms ASC"""
    )
    fun observeSchedule(channelId: String, fromMs: Long, toMs: Long): Flow<List<EpgProgrammeEntity>>
}
