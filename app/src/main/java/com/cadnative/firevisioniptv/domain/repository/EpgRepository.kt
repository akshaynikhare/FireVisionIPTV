package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.domain.model.EpgProgram
import java.time.Instant

interface EpgRepository {
    suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?>
    suspend fun ensureLoaded()

    /**
     * Force a refresh from all EPG sources now, bypassing the once-per-session guard.
     * Used by the background sync worker and manual "refresh guide" actions. Never
     * throws; on failure the last-good cache is retained.
     */
    suspend fun refreshNow()
    /** Returns null if EPG not yet loaded (caller should skip enrichment). Non-blocking. */
    fun getNowNextIfCached(tvgId: String): Pair<EpgProgram?, EpgProgram?>?

    /**
     * Programs overlapping the [from, to] window for each requested tvgId, sorted
     * by start time. Loads the guide if needed. Channels with no EPG data map to an
     * empty list. Backs the Guide grid's per-channel timeline rows.
     */
    suspend fun getProgramsWindow(
        tvgIds: List<String>,
        from: Instant,
        to: Instant
    ): Map<String, List<EpgProgram>>
}
