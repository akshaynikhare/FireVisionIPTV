package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.components.ChannelOverlay
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel

/**
 * The surfaces that layer above the video and take focus from it: the audio/subtitle
 * track picker and the channel list. Kept out of the screen root because both are
 * modal — they own focus while open, and closing one has to hand it back.
 *
 * Not composed in PiP, where there is no room for either and no way to drive them.
 */
@Composable
internal fun PlayerModals(
    uiState: PlayerUiState,
    state: PlayerOverlayState,
    viewModel: PlayerViewModel,
    exoPlayer: ExoPlayer,
    isMobile: Boolean,
    showTracksPanel: Boolean,
    onCloseTracksPanel: () -> Unit
) {
    if (showTracksPanel) {
        PlayerTracksPanel(
            exoPlayer = exoPlayer,
            onDismiss = {
                onCloseTracksPanel()
                // Return focus to the bar that launched the panel
                if (!isMobile) state.focusQuickActions()
            }
        )
    }

    ChannelOverlay(
        isVisible = uiState.showChannelOverlay,
        currentChannel = uiState.channel,
        recentChannels = uiState.recentChannels,
        overlayEpg = uiState.overlayEpg,
        channels = uiState.overlayChannels,
        categories = uiState.overlayCategories,
        selectedCategory = uiState.overlaySelectedCategory,
        isLoadingChannels = uiState.overlayIsLoadingChannels,
        isSwitchingChannel = uiState.isSwitchingChannel,
        nowProgram = uiState.nowPlaying,
        nextProgram = uiState.nextProgram,
        onChannelClick = { viewModel.switchChannel(it) },
        onCategorySelected = { viewModel.loadChannelList(it) },
        onFavoriteClick = { viewModel.toggleOverlayFavorite(it) },
        onInteraction = { viewModel.resetAutoHideTimer() },
        onDismiss = { viewModel.hideOverlay() },
        modifier = Modifier.fillMaxSize()
    )
}
