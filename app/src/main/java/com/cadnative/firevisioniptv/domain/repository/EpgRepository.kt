package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.domain.model.EpgProgram

interface EpgRepository {
    suspend fun getNowNext(tvgId: String): Pair<EpgProgram?, EpgProgram?>
}
