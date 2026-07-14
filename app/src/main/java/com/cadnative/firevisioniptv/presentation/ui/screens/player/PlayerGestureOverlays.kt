package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_EXIT
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BodyOverlay
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.softShadow

private val LevelBarShape = RoundedCornerShape(2.dp)

/** Brightness/volume pill shown mid-gesture on its own half of the screen. */
@Composable
internal fun BoxScope.GestureLevelIndicator(state: PlayerOverlayState) {
    val indicator = state.gestureIndicator ?: return
    val isBrightness = indicator.type == GestureIndicatorType.BRIGHTNESS
    AnimatedVisibility(
        visible = state.showGestureIndicator,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(if (isBrightness) Alignment.CenterStart else Alignment.CenterEnd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
            modifier = Modifier
                .padding(horizontal = Dimens.Space6)
                .clip(ShapeLarge)
                .background(ScrimHeavy)
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)
        ) {
            Icon(
                imageVector = if (isBrightness) Icons.Filled.BrightnessMedium else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isBrightness) "Brightness" else "Volume",
                tint = OnVideo,
                modifier = Modifier.size(Dimens.IconMedium)
            )
            Box(
                modifier = Modifier
                    .width(Dimens.GestureIndicatorWidth)
                    .height(Dimens.GestureIndicatorBarHeight)
                    .background(OnVideo.copy(alpha = 0.24f), LevelBarShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(indicator.level)
                        .background(Amber, LevelBarShape)
                )
            }
        }
    }
}

/** Brief channel confirmation pill after a swipe zap: logo, name, now-playing. */
@Composable
internal fun BoxScope.ZapOverlay(
    state: PlayerOverlayState,
    channel: ChannelUiModel?,
    nowTitle: String?
) {
    AnimatedVisibility(
        visible = state.showZapOverlay && channel != null,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = Dimens.Space6)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3),
            modifier = Modifier
                .clip(ShapeLarge)
                .background(ScrimHeavy)
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
        ) {
            Card(
                shape = ShapeSmall,
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier.size(Dimens.ChannelLogoSize)
            ) {
                AsyncImage(
                    model = channel?.logoUrl,
                    contentDescription = channel?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column {
                Text(
                    text = channel?.name.orEmpty(),
                    style = BodyOverlay,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!nowTitle.isNullOrBlank()) {
                    Text(
                        text = nowTitle,
                        style = LabelToast,
                        color = OnVideo.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Centered spinner while the live stream rebuffers (not during load/zap/recovery). */
@Composable
internal fun BoxScope.BufferingOverlay(uiState: PlayerUiState) {
    val visible = uiState.isBuffering &&
            uiState.channel != null &&
            !uiState.isLoading &&
            !uiState.isSwitchingChannel &&
            !uiState.isRecovering &&
            !uiState.isStreamDead
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Box(
            modifier = Modifier
                .softShadow(Elevation.Level3, ShapeLarge)
                .clip(ShapeLarge)
                .background(SurfaceElevated.copy(alpha = 0.85f))
                .padding(Dimens.Space4),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Amber,
                strokeWidth = 3.dp,
                modifier = Modifier.size(Dimens.IconLarge)
            )
        }
    }
}

/**
 * Screen-lock chip (Netflix pattern): while locked all chrome and gestures are
 * dead; tapping the video reveals this chip, tapping the chip arms it, tapping
 * again within the arm window unlocks.
 */
@Composable
internal fun BoxScope.LockChip(state: PlayerOverlayState) {
    AnimatedVisibility(
        visible = state.screenLocked && state.showLockChip,
        enter = fadeIn(tween(DURATION_FAST, easing = EaseOutQuart)),
        exit = fadeOut(tween(DURATION_EXIT, easing = EaseOutQuart)),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = Dimens.Space6)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            modifier = Modifier
                .clip(ShapeLarge)
                .background(ScrimHeavy)
                .clickable {
                    if (state.unlockArmed) {
                        state.unlockScreen()
                    } else {
                        state.unlockArmed = true
                        state.revealLockChip()
                    }
                }
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Screen locked",
                tint = if (state.unlockArmed) Amber else OnVideo,
                modifier = Modifier.size(Dimens.IconSmall)
            )
            Text(
                text = if (state.unlockArmed) "Tap again to unlock" else "Screen locked",
                style = LabelToast,
                color = OnVideo
            )
        }
    }
}
