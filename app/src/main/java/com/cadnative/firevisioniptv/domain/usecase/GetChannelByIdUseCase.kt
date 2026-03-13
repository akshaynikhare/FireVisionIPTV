package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific channel by its ID.
 *
 * This use case encapsulates the business logic for fetching a single channel
 * from the repository. It returns a Flow that emits updates whenever the channel
 * data changes (e.g., favorite status changes).
 *
 * The repository implements an offline-first strategy, so this use case will:
 * 1. Emit cached data immediately
 * 2. Automatically emit updated data when changes occur
 * 3. Continue to work offline with cached data
 *
 * Example usage in ViewModel:
 * ```
 * viewModelScope.launch {
 *     getChannelByIdUseCase(channelId).collect { result ->
 *         when (result) {
 *             is Result.Success -> updateUiState(result.data)
 *             is Result.Error -> showError(result.exception)
 *         }
 *     }
 * }
 * ```
 *
 * Requirements: TR-003 (Clean Architecture with Use Cases)
 */
class GetChannelByIdUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) : FlowUseCase<String, Result<Channel>>() {
    
    /**
     * Executes the use case to retrieve a specific channel.
     *
     * @param params The channel ID to retrieve
     * @return Flow emitting Result with the channel or error if not found
     */
    override fun execute(params: String): Flow<Result<Channel>> {
        return channelRepository.getChannelById(params)
    }
}
