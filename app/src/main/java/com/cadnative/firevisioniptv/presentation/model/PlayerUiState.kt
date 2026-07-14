package com.cadnative.firevisioniptv.presentation.model

import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction

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
    val deadStreamTitle: String = "",
    val deadStreamExplanation: String = "",
    val shouldNavigateBack: Boolean = false,
    // Channel overlay state
    val showChannelOverlay: Boolean = false,
    val overlayChannels: List<ChannelUiModel> = emptyList(),
    val overlayCategories: List<String> = emptyList(),
    val overlaySelectedCategory: String? = null,
    val overlayIsLoadingChannels: Boolean = false,
    val isSwitchingChannel: Boolean = false,
    val isUsingProxy: Boolean = false,
    val activeStreamUrl: String? = null,
    // EPG now/next state
    val nowPlaying: EpgProgram? = null,
    val nextProgram: EpgProgram? = null,
    // Now/next per overlay channel, keyed by normalized tvgId (trim + lowercase)
    val overlayEpg: Map<String, Pair<EpgProgram?, EpgProgram?>> = emptyMap(),
    // Bumped when the current channel's program transitions to the next one
    val programChangedToken: Int = 0,
    // How long the info banner lingers before auto-hiding (settings-driven)
    val infoBarTimeoutSeconds: Int = 4,
    // Current channel's day schedule (mobile portrait Schedule tab)
    val schedulePrograms: List<EpgProgram> = emptyList(),
    val scheduleLoading: Boolean = false,
    // Recently watched channels, most recent first (max 3, never the current one)
    val recentChannels: List<ChannelUiModel> = emptyList(),
    // Navigation preferences
    val keyUpDownAction: String = PlayerKeyAction.ZAP,
    val keyLeftRightAction: String = PlayerKeyAction.ZAP,
    val longOkAction: String = PlayerKeyAction.FAVORITE,
    val alwaysShowProgramBar: Boolean = false,
    // Sleep timer / auto-off
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemainingSeconds: Int? = null,
    val sleepTimerExpired: Boolean = false,
    val sleepTimerNavigateBack: Boolean = false
) {
    // Last watched channel (for quick recall)
    val lastChannel: ChannelUiModel? get() = recentChannels.firstOrNull()
}
