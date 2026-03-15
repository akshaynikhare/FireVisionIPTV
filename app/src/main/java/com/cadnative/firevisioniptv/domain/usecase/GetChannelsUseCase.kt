package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all channels.
 *
 * This use case encapsulates the business logic for fetching all available channels
 * from the repository. It returns a Flow that emits updates whenever the channel
 * data changes, enabling reactive UI updates.
 *
 * The repository implements an offline-first strategy, so this use case will:
 * 1. Emit cached data immediately
 * 2. Automatically emit updated data when remote fetch completes
 * 3. Continue to work offline with cached data
 *
 * Example usage in ViewModel:
 * ```
 * viewModelScope.launch {
 *     getChannelsUseCase(Unit).collect { result ->
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
class GetChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) : FlowUseCase<Unit, Result<List<Channel>>>() {
    
    /**
     * Executes the use case to retrieve all channels.
     *
     * @param params Unit (no parameters required)
     * @return Flow emitting Result with list of channels or error
     */
    override fun execute(params: Unit): Flow<Result<List<Channel>>> {
        return channelRepository.getChannels()
    }
}
