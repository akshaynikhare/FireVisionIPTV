package com.cadnative.firevisioniptv.presentation.model

/**
 * UI state for the channels screen.
 * 
 * Represents the complete state of the channels screen including
 * data, loading status, errors, and selected filters.
 */
data class ChannelsUiState(
    val channels: List<ChannelUiModel> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null
)
