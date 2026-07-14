package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimLight

// Top-anchored scrim so the top bar reads over bright video
private val TopBarScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.7f),
    0.6f to ScrimLight,
    1f to Color.Transparent
)

/** Callbacks for the mobile chrome, grouped to keep the signature small. */
internal class MobileChromeActions(
    val onBack: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onShowChannelList: () -> Unit,
    val onShowTracks: () -> Unit,
    val onCycleAspect: () -> Unit,
    val onCycleSleepTimer: (Int?) -> Unit,
    val onPrevChannel: () -> Unit,
    val onNextChannel: () -> Unit,
    val onEnterPip: () -> Unit,
    val onEnterFullscreen: () -> Unit,
    val onExitFullscreen: () -> Unit
)

/**
 * Mobile landscape chrome: top bar (back, channel + now-playing, tracks, PiP)
 * and bottom control row (prev / favorite / channels / aspect / sleep / lock /
 * next / exit-fullscreen). In portrait the detail + tabs sections below the
 * video own the actions, so the only video chrome is the fullscreen button.
 */
@Composable
internal fun BoxScope.PlayerMobileChrome(
    uiState: PlayerUiState,
    state: PlayerOverlayState,
    isPortrait: Boolean,
    controlsVisible: Boolean,
    actions: MobileChromeActions
) {
    val visible = controlsVisible && !state.screenLocked

    if (isPortrait) {
        // Docked-player mini chrome: just the fullscreen toggle
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.Space2)
        ) {
            ChromeIconButton(
                icon = Icons.Filled.Fullscreen,
                contentDescription = "Fullscreen",
                onClick = actions.onEnterFullscreen
            )
        }
        return
    }

    // ── Landscape: top bar ───────────────────────────────────────────
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.PlayerTopBarHeight)
                .background(TopBarScrim)
                .padding(horizontal = Dimens.Space3)
        ) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnVideo
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.channel?.name.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                uiState.nowPlaying?.let { now ->
                    Text(
                        text = now.title,
                        style = LabelToast,
                        color = OnVideo.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = actions.onShowTracks) {
                Icon(
                    imageVector = Icons.Filled.ClosedCaption,
                    contentDescription = "Audio and subtitles",
                    tint = OnVideo
                )
            }
            IconButton(onClick = actions.onEnterPip) {
                Icon(
                    imageVector = Icons.Filled.PictureInPictureAlt,
                    contentDescription = "Picture in picture",
                    tint = OnVideo
                )
            }
        }
    }

    // ── Landscape: bottom control row ────────────────────────────────
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = Dimens.Space6 * 3)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChromeIconButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous channel",
                onClick = actions.onPrevChannel
            )
            ChromeIconButton(
                icon = if (uiState.channel?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (uiState.channel?.isFavorite == true) MaterialTheme.colorScheme.error else OnVideo,
                onClick = actions.onToggleFavorite
            )
            ChromeIconButton(
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "Channel list",
                onClick = actions.onShowChannelList
            )
            ChromeIconButton(
                icon = Icons.Filled.AspectRatio,
                contentDescription = "Aspect ratio",
                onClick = actions.onCycleAspect
            )
            ChromeIconButton(
                icon = Icons.Filled.Bedtime,
                contentDescription = "Sleep timer",
                tint = if (uiState.sleepTimerMinutes != null) Amber else OnVideo,
                onClick = { actions.onCycleSleepTimer(nextSleepTimerStep(uiState.sleepTimerMinutes)) }
            )
            ChromeIconButton(
                icon = Icons.Filled.Lock,
                contentDescription = "Lock screen",
                onClick = { state.lockScreen() }
            )
            ChromeIconButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next channel",
                onClick = actions.onNextChannel
            )
            ChromeIconButton(
                icon = Icons.Filled.FullscreenExit,
                contentDescription = "Exit fullscreen",
                onClick = actions.onExitFullscreen
            )
        }
    }
}

@Composable
private fun ChromeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = OnVideo
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Dimens.PlayerControlButton)
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                shape = CircleShape
            )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
