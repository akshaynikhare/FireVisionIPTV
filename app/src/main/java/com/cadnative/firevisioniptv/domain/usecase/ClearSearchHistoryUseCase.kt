package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
import javax.inject.Inject

/**
 * Use case for clearing search history.
 * 
 * Removes all saved search queries from history.
 * 
 * Requirements: US-003.6 (Clear search history option)
 */
class ClearSearchHistoryUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) : UseCase<Unit, Result<Unit>>() {
    
    override suspend fun execute(params: Unit): Result<Unit> {
        return searchHistoryRepository.clearHistory()
    }
}
