package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import javax.inject.Inject

/**
 * Use case for refreshing channels from the remote source.
 *
 * This use case encapsulates the business logic for triggering a refresh of channel
 * data from the remote API. It performs a one-time operation that fetches fresh data
 * and updates the local cache.
 *
 * After a successful refresh, all Flow-based channel queries (GetChannelsUseCase,
 * GetChannelsByCategoryUseCase, etc.) will automatically emit the updated data.
 *
 * This use case is typically called when:
 * - User performs pull-to-refresh
 * - App starts and needs to sync data
 * - User explicitly requests a data refresh
 *
 * Example usage in ViewModel:
 * ```
 * viewModelScope.launch {
 *     _uiState.update { it.copy(isRefreshing = true) }
 *     val result = refreshChannelsUseCase(Unit)
 *     when (result) {
 *         is Result.Success -> _uiState.update { it.copy(isRefreshing = false) }
 *         is Result.Error -> showError(result.exception)
 *     }
 * }
 * ```
 *
 * Requirements: TR-003 (Clean Architecture with Use Cases)
 */
class RefreshChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) : UseCase<Unit, Result<Unit>>() {
    
    /**
     * Executes the use case to refresh channels from remote source.
     *
     * This operation:
     * 1. Fetches fresh data from the remote API
     * 2. Updates the local cache with the new data
     * 3. Returns success or error result
     *
     * @param params Unit (no parameters required)
     * @return Result indicating success or failure of the refresh operation
     */
    override suspend fun execute(params: Unit): Result<Unit> {
        return channelRepository.refreshChannels()
    }
}
