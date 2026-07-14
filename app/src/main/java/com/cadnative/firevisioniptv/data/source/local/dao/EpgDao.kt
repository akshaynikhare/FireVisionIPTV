package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cadnative.firevisioniptv.data.source.local.entity.EpgProgramEntity

/**
 * Data Access Object for EPG program operations.
 *
 * Backs the offline-first EPG cache: programs are upserted on refresh and read
 * back for the guide grid and the player now/next overlay. Reads never fail —
 * on a failed refresh the last-good rows remain.
 */
@Dao
interface EpgDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(programs: List<EpgProgramEntity>)

    /**
     * Programs overlapping the [fromMs, toMs] window for the requested channels.
     */
    @Query(
        """
        SELECT * FROM epg_programs
        WHERE channelEpgId IN (:channelEpgIds)
          AND startTimeMs < :toMs
          AND endTimeMs > :fromMs
        ORDER BY channelEpgId, startTimeMs
        """
    )
    suspend fun getProgramsInWindow(
        channelEpgIds: List<String>,
        fromMs: Long,
        toMs: Long
    ): List<EpgProgramEntity>

    /** All cached programs — used to hydrate the in-memory read cache at startup. */
    @Query("SELECT * FROM epg_programs ORDER BY channelEpgId, startTimeMs")
    suspend fun getAll(): List<EpgProgramEntity>

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun count(): Int

    /** Prune programs that ended before the cutoff. Only run after a successful refresh. */
    @Query("DELETE FROM epg_programs WHERE endTimeMs < :cutoffMs")
    suspend fun deleteEndedBefore(cutoffMs: Long)

    /** Wipe the whole guide — used by Settings → Clear Cache. */
    @Query("DELETE FROM epg_programs")
    suspend fun deleteAll()

    /** Delete current/future programs for the given channels — used to replace a stale schedule. */
    @Query("DELETE FROM epg_programs WHERE channelEpgId IN (:channelEpgIds) AND endTimeMs > :fromMs")
    suspend fun deleteUpcomingForChannels(channelEpgIds: List<String>, fromMs: Long)

    /**
     * Atomically replace the schedule for the refreshed channels: drop their current/future
     * rows (so programs the provider removed or rescheduled disappear), insert the fresh feed,
     * then prune long-past rows. Wrapped in a transaction so a partial failure can't leave Room
     * half-updated while the read cache serves the old schedule.
     */
    @Transaction
    suspend fun replaceForChannels(
        programs: List<EpgProgramEntity>,
        channelEpgIds: List<String>,
        fromMs: Long,
        prunedBeforeMs: Long
    ) {
        // Chunk the IN(...) delete so a large channel list can't exceed SQLite's
        // ~999 bound-parameter limit (a 5000-channel playlist would otherwise crash).
        channelEpgIds.chunked(SQLITE_MAX_IN_ARGS).forEach { chunk ->
            if (chunk.isNotEmpty()) deleteUpcomingForChannels(chunk, fromMs)
        }
        upsertAll(programs)
        deleteEndedBefore(prunedBeforeMs)
    }

    companion object {
        /** Kept well under SQLite's 999-variable ceiling to leave room for other bound args. */
        private const val SQLITE_MAX_IN_ARGS = 900
    }
}
