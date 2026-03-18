package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import javax.inject.Inject

class ReportStreamPlayUseCase @Inject constructor(
    private val repository: StreamMetricsRepository
) : UseCase<String, Result<Unit>>() {

    override suspend fun execute(params: String): Result<Unit> {
        return repository.reportStreamPlay(params)
    }
}
