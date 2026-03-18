package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.HealthSyncEntry
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import javax.inject.Inject

class SyncHealthResultsUseCase @Inject constructor(
    private val repository: StreamMetricsRepository
) : UseCase<List<HealthSyncEntry>, Result<Unit>>() {

    override suspend fun execute(params: List<HealthSyncEntry>): Result<Unit> {
        return repository.syncHealthResults(params)
    }
}
