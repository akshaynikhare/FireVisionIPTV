package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ZAP_DRAG_THRESHOLD_PX = 100f
private const val PINCH_ZOOM_IN_THRESHOLD = 1.15f
private const val PINCH_ZOOM_OUT_THRESHOLD = 0.85f
private const val MIN_BRIGHTNESS = 0.01f

/** Everything the gesture layer can trigger; kept together so the modifier signature stays sane. */
internal class PlayerGestureActions(
    val onToggleChrome: () -> Unit,
    val onPlayPause: () -> Unit,
    val onLongPressFavorite: () -> Unit,
    val onZap: (forward: Boolean) -> Unit,
    val onPinchAspect: (zoomIn: Boolean) -> Unit,
    val onDismissOverlay: () -> Unit,
    val onCancelSleepExpiry: () -> Unit
)

/**
 * Mobile touch engine for the video surface. One tap detector (tap = chrome
 * toggle, double-tap = play/pause, long-press = favorite) plus one custom
 * drag/pinch detector that locks a single mode per gesture: two fingers =
 * pinch (aspect), horizontal = channel zap, vertical = brightness (left half)
 * or volume (right half) with indicator pills.
 *
 * While [PlayerOverlayState.screenLocked], everything is dead except a tap
 * that re-reveals the lock chip.
 */
internal fun Modifier.playerGestures(
    state: PlayerOverlayState,
    exoPlayer: ExoPlayer,
    activity: Activity?,
    audioManager: AudioManager,
    haptic: HapticFeedback,
    overlayVisible: Boolean,
    sleepTimerExpired: Boolean,
    actions: PlayerGestureActions
): Modifier {
    if (state.screenLocked) {
        return pointerInput(true) {
            detectTapGestures(onTap = { state.revealLockChip() })
        }
    }
    return this
        .pointerInput(overlayVisible, sleepTimerExpired) {
            coroutineScope {
                launch {
                    detectTapGestures(
                        onTap = {
                            when {
                                sleepTimerExpired -> {
                                    actions.onCancelSleepExpiry()
                                    exoPlayer.play()
                                }
                                overlayVisible -> actions.onDismissOverlay()
                                else -> actions.onToggleChrome()
                            }
                        },
                        onDoubleTap = {
                            if (!overlayVisible && !sleepTimerExpired) actions.onPlayPause()
                        },
                        onLongPress = {
                            if (!overlayVisible && !sleepTimerExpired) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                actions.onLongPressFavorite()
                            }
                        }
                    )
                }
                launch {
                    detectDragAndPinch(
                        enabled = { !overlayVisible && !sleepTimerExpired },
                        state = state,
                        activity = activity,
                        audioManager = audioManager,
                        haptic = haptic,
                        actions = actions
                    )
                }
            }
        }
}

private enum class DragMode { UNDECIDED, HORIZONTAL, BRIGHTNESS, VOLUME, PINCH }

private suspend fun PointerInputScope.detectDragAndPinch(
    enabled: () -> Boolean,
    state: PlayerOverlayState,
    activity: Activity?,
    audioManager: AudioManager,
    haptic: HapticFeedback,
    actions: PlayerGestureActions
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled()) return@awaitEachGesture
        var mode = DragMode.UNDECIDED
        var slopX = 0f
        var slopY = 0f
        var totalDragX = 0f
        var zoomAccum = 1f
        var level = 0f // brightness or volume fraction while in a vertical mode
        val touchSlop = viewConfiguration.touchSlop

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break

            if (pressed.size >= 2 && (mode == DragMode.UNDECIDED || mode == DragMode.PINCH)) {
                mode = DragMode.PINCH
                zoomAccum *= event.calculateZoom()
                event.changes.forEach { it.consume() }
                continue
            }

            val change = pressed.first()
            val delta = change.positionChange()
            when (mode) {
                DragMode.UNDECIDED -> {
                    slopX += delta.x
                    slopY += delta.y
                    if (abs(slopX) > touchSlop || abs(slopY) > touchSlop) {
                        mode = when {
                            abs(slopX) > abs(slopY) -> DragMode.HORIZONTAL
                            down.position.x < size.width / 2f -> DragMode.BRIGHTNESS
                            else -> DragMode.VOLUME
                        }
                        when (mode) {
                            DragMode.HORIZONTAL -> totalDragX = slopX
                            DragMode.BRIGHTNESS -> level = currentBrightness(activity)
                            DragMode.VOLUME -> level = currentVolumeFraction(audioManager)
                            else -> {}
                        }
                        change.consume()
                    }
                }
                DragMode.HORIZONTAL -> {
                    totalDragX += delta.x
                    change.consume()
                }
                DragMode.BRIGHTNESS -> {
                    level = (level - delta.y / size.height).coerceIn(MIN_BRIGHTNESS, 1f)
                    applyBrightness(activity, level)
                    state.showGestureLevel(GestureIndicatorType.BRIGHTNESS, level)
                    change.consume()
                }
                DragMode.VOLUME -> {
                    level = (level - delta.y / size.height).coerceIn(0f, 1f)
                    applyVolumeFraction(audioManager, level)
                    state.showGestureLevel(GestureIndicatorType.VOLUME, level)
                    change.consume()
                }
                DragMode.PINCH -> { /* fingers lifted back to one — ride it out */ }
            }
        }

        when (mode) {
            DragMode.HORIZONTAL -> {
                val now = System.currentTimeMillis()
                if (abs(totalDragX) > ZAP_DRAG_THRESHOLD_PX &&
                    now - state.lastChannelSwitchTime >= CHANNEL_SWITCH_DEBOUNCE_MS
                ) {
                    state.lastChannelSwitchTime = now
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    // Drag right = previous channel, drag left = next
                    actions.onZap(totalDragX < 0)
                    state.flashZapOverlay()
                }
            }
            DragMode.PINCH -> {
                if (zoomAccum >= PINCH_ZOOM_IN_THRESHOLD) actions.onPinchAspect(true)
                else if (zoomAccum <= PINCH_ZOOM_OUT_THRESHOLD) actions.onPinchAspect(false)
            }
            else -> {}
        }
    }
}

private fun currentBrightness(activity: Activity?): Float {
    val window = activity?.window ?: return 0.5f
    val override = window.attributes.screenBrightness
    if (override >= 0f) return override
    return try {
        Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    } catch (_: Exception) {
        0.5f
    }
}

private fun applyBrightness(activity: Activity?, level: Float) {
    val window = activity?.window ?: return
    val attrs = window.attributes
    attrs.screenBrightness = level
    window.attributes = attrs
}

private fun currentVolumeFraction(audioManager: AudioManager): Float {
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (max <= 0) return 0f
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun applyVolumeFraction(audioManager: AudioManager, fraction: Float) {
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    // Flag 0: adjust silently — the in-app indicator pill is the feedback
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).roundToInt(), 0)
}
