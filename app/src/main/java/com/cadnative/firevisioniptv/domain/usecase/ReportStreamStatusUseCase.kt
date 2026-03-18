package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import javax.inject.Inject

class ReportStreamStatusUseCase @Inject constructor(
    private val repository: StreamMetricsRepository
) : UseCase<ReportStreamStatusUseCase.Params, Result<Unit>>() {

    data class Params(
        val channelId: String,
        val status: Status,
        val errorMessage: String? = null
    )

    enum class Status { DEAD, ALIVE, UNRESPONSIVE }

    override suspend fun execute(params: Params): Result<Unit> {
        return when (params.status) {
            Status.DEAD -> repository.reportStreamDead(params.channelId, params.errorMessage)
            Status.ALIVE -> repository.reportStreamAlive(params.channelId)
            Status.UNRESPONSIVE -> repository.reportStreamUnresponsive(params.channelId)
        }
    }
}
