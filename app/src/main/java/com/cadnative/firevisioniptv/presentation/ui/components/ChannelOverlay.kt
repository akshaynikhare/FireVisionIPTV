package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.softShadow

@Composable
fun ChannelOverlay(
    isVisible: Boolean,
    currentChannel: ChannelUiModel?,
    lastChannel: ChannelUiModel? = null,
    channels: List<ChannelUiModel>,
    categories: List<String>,
    selectedCategory: String?,
    isLoadingChannels: Boolean,
    isSwitchingChannel: Boolean,
    nowProgram: EpgProgram? = null,
    nextProgram: EpgProgram? = null,
    onChannelClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onInteraction: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent {
                onInteraction()
                false // don't consume — let focus system handle navigation
            }
    ) {
        // Scrim
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            )
        }

        // Channel switching indicator
        AnimatedVisibility(
            visible = isSwitchingChannel,
            enter = fadeIn(tween(DURATION_NORMAL)),
            exit = fadeOut(tween(DURATION_NORMAL)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Lift the spinner onto an elevated chip so it reads above the video
            // frame during a channel switch instead of floating on bare pixels.
            Box(
                modifier = Modifier
                    .softShadow(Elevation.Level3, MaterialTheme.shapes.large)
                    .clip(MaterialTheme.shapes.large)
                    .background(SurfaceElevated.copy(alpha = 0.85f))
                    .padding(Dimens.Space4),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Amber,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Info bar + channel panel — slide up together
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(DURATION_ENTRANCE, easing = EaseOutQuart)
            ) + fadeIn(tween(DURATION_ENTRANCE, easing = EaseOutQuart)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(DURATION_EXIT, easing = EaseOutQuart)
            ) + fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlayerInfoBar(
                    channel = currentChannel,
                    nowPlaying = nowProgram,
                    nextProgram = nextProgram
                )
                BottomChannelPanel(
                    currentChannel = currentChannel,
                    lastChannel = lastChannel,
                    channels = channels,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    isLoading = isLoadingChannels,
                    isVisible = isVisible,
                    onChannelClick = onChannelClick,
                    onCategorySelected = onCategorySelected,
                    onFavoriteClick = onFavoriteClick,
                    onDismiss = onDismiss
                )
            }
        }
    }
}
