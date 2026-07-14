package com.cadnative.firevisioniptv.presentation.ui.screens.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cadnative.firevisioniptv.ComposeMainActivity
import com.cadnative.firevisioniptv.PipController
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.source.remote.playlist.StreamUrlTemplate
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.player.ErrorRecoveryManager
import com.cadnative.firevisioniptv.presentation.ui.player.isTvDevice
import com.cadnative.firevisioniptv.presentation.viewmodel.PlayerViewModel
import java.net.URLEncoder

/**
 * Point the player at a channel: builds the live stream slots (primary +
 * alternates, each with an optional proxy fallback) or the Xtream catch-up
 * archive URL, arms the recovery manager, and prepares playback.
 * Returns false when the channel has no usable stream URL.
 */
internal fun prepareChannelStream(
    context: Context,
    exoPlayer: ExoPlayer,
    errorRecoveryManager: ErrorRecoveryManager,
    channel: ChannelUiModel,
    catchupStartMs: Long,
    catchupDurationMin: Int
): Boolean {
    val url = channel.streamUrl?.let { StreamUrlTemplate.resolve(context, it) }
    if (url.isNullOrEmpty()) return false
    errorRecoveryManager.reset()

    // Catch-up: play the Xtream timeshift archive URL for a past program.
    val catchupUrl = if (catchupStartMs > 0) {
        buildCatchupUrl(context, channel.id, catchupStartMs, catchupDurationMin)
    } else null

    if (catchupUrl != null) {
        // Single archive stream — no live alternates/proxy.
        errorRecoveryManager.setStreamSlots(
            listOf(ErrorRecoveryManager.StreamSlot(catchupUrl, null, isPrimary = true))
        )
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(catchupUrl).build())
    } else {
        // Build stream slots: primary + alternates, each with optional proxy
        val serverUrl = AppPreferences.getServerUrl(context).trimEnd('/')
        val tvCode = AppPreferences.getTvCode(context)
        val canProxy = tvCode.isNotEmpty() && !AppPreferences.isDemoMode(context)

        fun buildProxyUrl(streamUrl: String): String? {
            if (!canProxy) return null
            return "$serverUrl/api/v1/tv/stream/$tvCode?url=${URLEncoder.encode(streamUrl, "UTF-8")}"
        }

        val slots = mutableListOf<ErrorRecoveryManager.StreamSlot>()
        slots.add(ErrorRecoveryManager.StreamSlot(url, buildProxyUrl(url), isPrimary = true))
        channel.alternateStreamUrls.take(3).forEach { altUrl ->
            slots.add(ErrorRecoveryManager.StreamSlot(altUrl, buildProxyUrl(altUrl), isPrimary = false))
        }
        errorRecoveryManager.setStreamSlots(slots)

        exoPlayer.setMediaItem(MediaItem.Builder().setUri(url).build())
    }
    exoPlayer.prepare()
    return true
}

/**
 * Build an Xtream catch-up (timeshift) URL for a past program:
 * `{host}/timeshift/{user}/{pass}/{durationMin}/{yyyy-MM-dd:HH-mm}/{streamId}.m3u8`.
 * Returns null when the source isn't Xtream or credentials are missing.
 */
private fun buildCatchupUrl(
    context: Context,
    channelId: String,
    startMs: Long,
    durationMin: Int
): String? {
    if (!channelId.startsWith("xtream-")) return null
    val host = AppPreferences.getXtreamHost(context).trimEnd('/')
    val user = AppPreferences.getXtreamUser(context)
    val pass = AppPreferences.getXtreamPass(context)
    if (host.isBlank() || user.isBlank()) return null
    val streamId = channelId.removePrefix("xtream-")
    val duration = durationMin.coerceAtLeast(1)
    val start = java.time.Instant.ofEpochMilli(startMs)
        .atZone(java.time.ZoneOffset.UTC)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm"))
    return "$host/timeshift/$user/$pass/$duration/$start/$streamId.m3u8"
}

/**
 * Playback listener lifecycle: buffering + play-state into the ViewModel,
 * PiP params refresh on mobile, thumbnail capture + player release on dispose.
 */
@Composable
internal fun PlayerPlaybackListenerEffect(
    exoPlayer: ExoPlayer,
    viewModel: PlayerViewModel,
    isMobile: Boolean,
    pipController: PipController?,
    errorRecoveryManager: ErrorRecoveryManager,
    getPlayerView: () -> PlayerView?,
    shouldCaptureThumbnail: () -> Boolean
) {
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                viewModel.updateBufferingState(playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.updatePlaybackState(
                        isPlaying = false,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                ComposeMainActivity.isPlayerPlaying = isPlaying
                if (isMobile) pipController?.update(isPlaying, canZap = true)
                viewModel.updatePlaybackState(
                    isPlaying = isPlaying,
                    position = exoPlayer.currentPosition,
                    duration = exoPlayer.duration.coerceAtLeast(0)
                )
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            // Capture thumbnail before releasing player
            if (shouldCaptureThumbnail()) {
                capturePlayerThumbnail(getPlayerView())?.let { bitmap ->
                    viewModel.saveThumbnailFromPlayer(bitmap)
                }
            }
            exoPlayer.removeListener(listener)
            errorRecoveryManager.release()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }
}

/** On TV devices: pause playback when backgrounded, resume when foregrounded. */
@Composable
internal fun TvBackgroundPauseEffect(exoPlayer: ExoPlayer) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasPlayingBeforeStop by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, exoPlayer) {
        if (!isTvDevice(context)) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        wasPlayingBeforeStop = exoPlayer.isPlaying
                        exoPlayer.pause()
                    }
                    Lifecycle.Event.ON_START -> {
                        if (wasPlayingBeforeStop) exoPlayer.play()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
}

/** PiP RemoteActions (prev/next channel) route into the ViewModel while composed. */
@Composable
internal fun PipRemoteActionsEffect(
    isMobile: Boolean,
    pipController: PipController?,
    viewModel: PlayerViewModel
) {
    DisposableEffect(Unit) {
        if (isMobile && pipController != null) {
            pipController.attach()
            pipController.onPipAction = { action ->
                when (action) {
                    PipController.PipAction.PREV_CHANNEL -> viewModel.previousChannel()
                    PipController.PipAction.NEXT_CHANNEL -> viewModel.nextChannel()
                }
            }
        }
        onDispose {
            if (isMobile && pipController != null) {
                pipController.onPipAction = null
                pipController.clearAutoEnter() // browsing screens must never auto-PiP
            }
        }
    }
}
