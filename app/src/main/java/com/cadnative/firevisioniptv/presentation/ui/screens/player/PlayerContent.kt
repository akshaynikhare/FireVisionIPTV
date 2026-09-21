package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.graphics.Bitmap
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_ENTRANCE
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.overlayEnter
import com.cadnative.firevisioniptv.presentation.ui.animation.overlayExit
import com.cadnative.firevisioniptv.presentation.ui.components.DeadStreamOverlay
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.components.LoadingIndicator
import com.cadnative.firevisioniptv.presentation.ui.components.RecoveringOverlay
import com.cadnative.firevisioniptv.R
import androidx.compose.ui.res.stringResource

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayer(
    exoPlayer: ExoPlayer,
    onPlayerViewCreated: (PlayerView) -> Unit,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                setResizeMode(resizeMode)
                keepScreenOn = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                onPlayerViewCreated(this)
            }
        },
        update = { it.setResizeMode(resizeMode) },
        modifier = modifier
    )
}

/** Aspect/zoom modes cycled by the player's Aspect quick-action, with labels. */
@OptIn(UnstableApi::class)
internal val ASPECT_MODES: List<Pair<Int, String>> = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill"
)

/** Loading/error crossfade plus stream recovery and dead-stream overlays. */
@Composable
internal fun PlayerStateOverlays(
    uiState: PlayerUiState,
    maxRecoveryAttempts: Int,
    onRetry: () -> Unit,
    onDismissDeadStream: () -> Unit
) {
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val contentState = when {
        uiState.isLoading -> "loading"
        uiState.error != null && !uiState.isStreamDead && !uiState.isRecovering -> "error"
        else -> "none"
    }

    Crossfade(
        targetState = contentState,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "playerState"
    ) { state ->
        when (state) {
            "loading" -> LoadingIndicator(message = "Loading channel...")
            "error" -> ErrorState(
                message = uiState.error ?: stringResource(R.string.player_load_failed),
                onRetry = onRetry
            )
            else -> { }
        }
    }

    AnimatedVisibility(
        visible = uiState.isRecovering,
        enter = overlayEnter(reduceMotion),
        exit = overlayExit(reduceMotion, DURATION_NORMAL)
    ) {
        RecoveringOverlay(attempt = uiState.recoveryAttempt, maxAttempts = maxRecoveryAttempts)
    }

    AnimatedVisibility(
        visible = uiState.isStreamDead,
        enter = overlayEnter(reduceMotion, DURATION_ENTRANCE),
        exit = overlayExit(reduceMotion)
    ) {
        val message = uiState.deadStreamMessage
        DeadStreamOverlay(
            title = message?.let { stringResource(it.titleRes) }
                ?: stringResource(R.string.stream_err_generic_title),
            explanation = message?.let { m ->
                listOfNotNull(
                    stringResource(m.explanationRes),
                    m.recentSuffixRes?.let { stringResource(it) }
                ).joinToString(" ")
            }.orEmpty(),
            countdown = uiState.deadStreamCountdown,
            onDismiss = onDismissDeadStream
        )
    }
}

/**
 * Capture a bitmap from the PlayerView's TextureView surface.
 * Checks that the TextureView is still available before attempting capture,
 * since the surface may already be released during dispose.
 */
@OptIn(UnstableApi::class)
internal fun capturePlayerThumbnail(playerView: PlayerView?): Bitmap? {
    return try {
        val textureView = playerView?.videoSurfaceView as? TextureView
        if (textureView != null && textureView.isAvailable) {
            textureView.bitmap
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
