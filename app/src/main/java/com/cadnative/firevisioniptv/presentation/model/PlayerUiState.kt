package com.cadnative.firevisioniptv.presentation.model

/**
 * UI state for the player screen.
 *
 * Represents the complete state of the video player including
 * channel information, playback state, and stream recovery.
 */
data class PlayerUiState(
    val channel: ChannelUiModel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val error: String? = null,
    // Stream recovery state
    val isRecovering: Boolean = false,
    val recoveryAttempt: Int = 0,
    val isStreamDead: Boolean = false,
    val deadStreamCountdown: Int = 0,
    val deadStreamMessage: String = "",
    val shouldNavigateBack: Boolean = false,
    // Channel overlay state
    val showChannelOverlay: Boolean = false,
    val overlayChannels: List<ChannelUiModel> = emptyList(),
    val overlayCategories: List<String> = emptyList(),
    val overlaySelectedCategory: String? = null,
    val overlayIsLoadingChannels: Boolean = false,
    val isSwitchingChannel: Boolean = false
)
