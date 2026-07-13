package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Fetches the program timeline for a set of channels over a time window, keyed by
 * tvgId, for the EPG Guide grid. The window is already cached in memory by
 * [EpgRepository] (the server returns a multi-hour guide), so this is a fast filter.
 */
class GetGuideProgramsUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) : UseCase<GetGuideProgramsUseCase.Params, Map<String, List<EpgProgram>>>() {

    data class Params(
        val tvgIds: List<String>,
        val from: Instant,
        val to: Instant
    )

    override suspend fun execute(params: Params): Map<String, List<EpgProgram>> =
        epgRepository.getProgramsWindow(params.tvgIds, params.from, params.to)
}
