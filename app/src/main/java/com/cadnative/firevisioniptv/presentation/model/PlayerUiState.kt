package com.cadnative.firevisioniptv.presentation.model

/**
 * UI state for the player screen.
 * 
 * Represents the complete state of the video player including
 * channel information, playback state, and control visibility.
 */
data class PlayerUiState(
    val channel: ChannelUiModel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val showControls: Boolean = true,
    val error: String? = null
)
