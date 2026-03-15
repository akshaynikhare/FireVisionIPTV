package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving recent search queries.
 * 
 * Returns the most recent search queries ordered by timestamp.
 * 
 * Requirements: US-003.3 (Recent searches saved locally)
 */
class GetRecentSearchesUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) : FlowUseCase<Int, Result<List<String>>>() {
    
    override fun execute(params: Int): Flow<Result<List<String>>> {
        return searchHistoryRepository.getRecentSearches(params)
    }
}
