package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.domain.model.EpgProgram

interface EpgRepository {
    suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?>
    suspend fun ensureLoaded()
    /** Returns null if EPG not yet loaded (caller should skip enrichment). Non-blocking. */
    fun getNowNextIfCached(tvgId: String): Pair<EpgProgram?, EpgProgram?>?
}
