package com.cadnative.firevisioniptv.presentation.model

/**
 * UI state for the favorites screen.
 * 
 * Represents the complete state of the favorites screen including
 * favorite channels, loading status, and errors.
 */
data class FavoritesUiState(
    val favorites: List<ChannelUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
