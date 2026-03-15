package com.cadnative.firevisioniptv.presentation.model

import com.cadnative.firevisioniptv.domain.model.SearchFilter

/**
 * UI state for the search screen.
 * 
 * Represents the complete state of the search screen including
 * search query, results, recent searches, and active filters.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<ChannelUiModel> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val activeFilters: List<SearchFilter> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
