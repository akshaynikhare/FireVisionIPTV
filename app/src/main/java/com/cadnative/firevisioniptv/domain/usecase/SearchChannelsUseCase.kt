package com.cadnative.firevisioniptv.domain.usecase

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.SearchFilter
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for searching channels with optional filters.
 *
 * This use case encapsulates the business logic for searching channels
 * and applying various filters to the results. It supports filtering by
 * category, language, country, or any combination of these criteria.
 *
 * The use case:
 * 1. Searches channels via the repository using the query string
 * 2. Applies the specified filters to the search results
 * 3. Returns a Flow that emits filtered results
 *
 * Example usage in ViewModel:
 * ```
 * val params = SearchChannelsUseCase.Params(
 *     query = "news",
 *     filters = listOf(
 *         SearchFilter.ByLanguage("en"),
 *         SearchFilter.ByCategory("News")
 *     )
 * )
 * viewModelScope.launch {
 *     searchChannelsUseCase(params).collect { result ->
 *         when (result) {
 *             is Result.Success -> updateSearchResults(result.data)
 *             is Result.Error -> showError(result.exception)
 *         }
 *     }
 * }
 * ```
 *
 * Requirements: US-003 (Improved Search Experience), US-009 (Advanced Filtering)
 */
class SearchChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) : FlowUseCase<SearchChannelsUseCase.Params, Result<List<Channel>>>() {
    
    /**
     * Parameters for the search operation.
     *
     * @param query The search query string
     * @param filters List of filters to apply to search results (default: empty)
     */
    data class Params(
        val query: String,
        val filters: List<SearchFilter> = emptyList()
    )
    
    /**
     * Executes the search with filters.
     *
     * @param params Search parameters including query and filters
     * @return Flow emitting Result with filtered channel list or error
     */
    override fun execute(params: Params): Flow<Result<List<Channel>>> {
        return channelRepository.searchChannels(params.query)
            .map { result ->
                when (result) {
                    is Result.Success -> Result.Success(applyFilters(result.data, params.filters))
                    is Result.Error -> result
                }
            }
    }
    
    /**
     * Applies the specified filters to the channel list.
     *
     * Filters are applied with AND logic - a channel must match all filters
     * to be included in the results. If no filters are provided, all channels
     * are returned unchanged.
     *
     * @param channels The list of channels to filter
     * @param filters The list of filters to apply
     * @return Filtered list of channels
     */
    private fun applyFilters(channels: List<Channel>, filters: List<SearchFilter>): List<Channel> {
        if (filters.isEmpty()) return channels
        
        return channels.filter { channel ->
            filters.all { filter ->
                when (filter) {
                    is SearchFilter.ByCategory -> channel.category == filter.category
                    is SearchFilter.ByLanguage -> channel.language == filter.language
                    is SearchFilter.ByCountry -> channel.country == filter.country
                    is SearchFilter.Combined -> applyFilters(listOf(channel), filter.filters).isNotEmpty()
                }
            }
        }
    }
}
