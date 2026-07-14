package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.OverlayToast
import com.cadnative.firevisioniptv.presentation.ui.components.PlayerInfoBar
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BodyOverlay
import com.cadnative.firevisioniptv.presentation.ui.theme.DisplayNumberChip
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimLight
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium

/**
 * All transient overlays layered above the video: info bar, toasts,
 * number chip, favorite button/indicator, play/pause flash, mobile controls.
 */
@Composable
internal fun BoxScope.PlayerOverlays(
    uiState: PlayerUiState,
    state: PlayerOverlayState,
    isMobile: Boolean,
    alwaysShowInfoBar: Boolean,
    aspectLabel: String,
    quickActionsFocusRequester: FocusRequester,
    onToggleFavorite: () -> Unit,
    onCycleSleepTimer: (Int?) -> Unit,
    onCycleAspect: () -> Unit,
    onShowTracks: () -> Unit,
    onShowChannelList: () -> Unit
) {
    val playbackHealthy = !uiState.isStreamDead && !uiState.isRecovering
    val controlsVisible = uiState.channel != null &&
            !uiState.showChannelOverlay &&
            playbackHealthy &&
            state.showControls
    val canShowInfoBar = uiState.channel != null &&
            !uiState.showChannelOverlay &&
            playbackHealthy
    // Full bar is revealed transiently on zap/INFO; the compact bar stays pinned
    // when enabled and the full bar isn't currently showing.
    val fullInfoBarVisible = state.showInfoBar && canShowInfoBar
    val pinnedInfoBarVisible = alwaysShowInfoBar && !state.showInfoBar && canShowInfoBar
    val infoBarVisible = fullInfoBarVisible || pinnedInfoBarVisible
    // Keep bottom-center toasts above the info bar when both are on screen
    val toastBottomPadding = if (infoBarVisible) 96.dp else 32.dp

    // Now/next info bar — revealed on zap or INFO key, auto-hides
    AnimatedVisibility(
        visible = fullInfoBarVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PlayerInfoBar(
            channel = uiState.channel,
            nowPlaying = uiState.nowPlaying,
            nextProgram = uiState.nextProgram,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Pinned compact live-EPG strip — always on when enabled in Settings
    AnimatedVisibility(
        visible = pinnedInfoBarVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PlayerInfoBar(
            channel = uiState.channel,
            nowPlaying = uiState.nowPlaying,
            nextProgram = uiState.nextProgram,
            modifier = Modifier.fillMaxWidth(),
            compact = true
        )
    }

    // Play/pause flash — brief centered icon after a play/pause action
    AnimatedVisibility(
        visible = state.showPlayPauseFlash && uiState.channel != null,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Icon(
            imageVector = if (state.playPauseFlashPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = null,
            tint = OnVideo,
            modifier = Modifier
                .clip(CircleShape)
                .background(ScrimHeavy)
                .padding(18.dp)
                .size(44.dp)
        )
    }

    // Favorite indicator — centered brief toast after toggle
    AnimatedVisibility(
        visible = state.showFavIndicator && uiState.channel != null,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        val isFav = uiState.channel?.isFavorite == true
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(ScrimHeavy)
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (isFav) MaterialTheme.colorScheme.error else OnVideo,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (isFav) "Added to Favorites" else "Removed from Favorites",
                style = BodyOverlay,
                color = OnVideo
            )
        }
    }

    // Sleep timer countdown chip — small, top-left, only in the final minute
    val sleepRemaining = uiState.sleepTimerRemainingSeconds
    AnimatedVisibility(
        visible = sleepRemaining != null && sleepRemaining in 1..60 && !uiState.sleepTimerExpired,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(32.dp)
    ) {
        OverlayToast("Sleep in ${sleepRemaining ?: 0}s")
    }

    // Sleep timer expired — "Still watching?" prompt with a cancel window
    AnimatedVisibility(
        visible = uiState.sleepTimerExpired,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        OverlayToast("Still watching? Press any button to continue")
    }

    // Channel number entry — top-right while typing
    if (state.numberBuffer.isNotEmpty()) {
        Text(
            text = state.numberBuffer,
            style = DisplayNumberChip,
            color = OnVideo,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .clip(ShapeMedium)
                .background(ScrimLight)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }

    // Key hints — bottom-center on the first few launches
    AnimatedVisibility(
        visible = state.showKeyHints && !uiState.showChannelOverlay,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = toastBottomPadding)
    ) {
        OverlayToast("OK: channels   ▲▼ ◀▶: zap   MENU: options   Hold OK: favorite   0-9: channel #")
    }

    // Live quick-actions bar (TV) — bottom-left, auto-hides with the other
    // transient controls. Lifts above the info bar so it never overlaps the
    // channel logo. On mobile the dedicated MobilePlayerControls own the chrome.
    AnimatedVisibility(
        visible = (controlsVisible || (state.controlsFocused && uiState.channel != null &&
                !uiState.showChannelOverlay && playbackHealthy)) && !isMobile,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 24.dp, bottom = if (infoBarVisible) 120.dp else 24.dp)
    ) {
        PlayerQuickActions(
            isFavorite = uiState.channel?.isFavorite == true,
            sleepTimerMinutes = uiState.sleepTimerMinutes,
            aspectLabel = aspectLabel,
            firstActionFocusRequester = quickActionsFocusRequester,
            onFocusStateChanged = { state.controlsFocused = it },
            onToggleFavorite = onToggleFavorite,
            onCycleSleepTimer = onCycleSleepTimer,
            onCycleAspect = onCycleAspect,
            onShowTracks = onShowTracks,
            onShowChannelList = onShowChannelList
        )
    }

    // Mobile keeps its own favorite button so long-press + tap chrome stays intact.
    if (isMobile) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
            exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = if (infoBarVisible) 120.dp else 24.dp)
        ) {
            FavoriteButton(
                isFavorite = uiState.channel?.isFavorite == true,
                onClick = onToggleFavorite
            )
        }
        MobilePlayerControls(
            controlsVisible = controlsVisible,
            onShowChannelList = onShowChannelList
        )
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.82f
            isFocused -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favBtnScale"
    )

    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 3f else 0f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "favBtnBorder"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (borderWidth > 0f) Modifier.border(
                    BorderStroke(borderWidth.dp, Amber),
                    CircleShape
                ) else Modifier
            )
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
        )
    }
}

/** Mobile-only controls: channel list button + rotation lock button. */
@Composable
private fun BoxScope.MobilePlayerControls(
    controlsVisible: Boolean,
    onShowChannelList: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    var isRotationLocked by remember { mutableStateOf(false) }

    // Channel list button — bottom-center (replaces double-tap for overlay)
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onShowChannelList,
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "Channel list",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Rotation button — bottom-right
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = {
                activity?.let {
                    if (isRotationLocked) {
                        it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        isRotationLocked = false
                    } else {
                        it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        isRotationLocked = true
                    }
                }
            },
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.ScreenRotation,
                contentDescription = if (isRotationLocked) "Unlock rotation" else "Lock to landscape",
                tint = if (isRotationLocked) Amber else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
