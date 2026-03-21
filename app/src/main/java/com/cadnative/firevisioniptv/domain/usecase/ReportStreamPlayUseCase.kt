package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import javax.inject.Inject

class ReportStreamPlayUseCase @Inject constructor(
    private val repository: StreamMetricsRepository
) : UseCase<ReportStreamPlayUseCase.Params, Result<Unit>>() {

    data class Params(
        val channelId: String,
        val proxyPlay: Boolean = false,
        val streamUrl: String? = null
    )

    override suspend fun execute(params: Params): Result<Unit> {
        return repository.reportStreamPlay(params.channelId, params.proxyPlay, params.streamUrl)
    }
}
