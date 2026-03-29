package com.cabletech.iptvplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.cabletech.iptvplayer.core.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core IPTV player backed by Media3 ExoPlayer.
 *
 * Buffer parameters are tuned for live IPTV streams (lower buffer target to
 * minimise channel-change latency while still absorbing network jitter).
 *
 * Exposes a [StateFlow] of [PlayerState] so the UI layer can observe without
 * holding a direct player reference.
 */
class IptvPlayer(context: Context) {

    // --- ExoPlayer with live-stream-tuned LoadControl ---

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(buildLiveLoadControl())
        .build()
        .also { it.addListener(PlayerListener()) }

    // --- State ---

    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentChannel: Channel? = null
    private var retryCount = 0
    private val maxRetries = 3

    // --- Public API ---

    fun play(channel: Channel) {
        retryCount = 0
        currentChannel = channel
        _state.value = PlayerState.Loading(channel)
        val item = MediaItem.fromUri(channel.streamUrl)
        exoPlayer.apply {
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        currentChannel?.let { _state.value = PlayerState.Paused(it) }
    }

    fun resume() {
        exoPlayer.playWhenReady = true
        currentChannel?.let { _state.value = PlayerState.Playing(it) }
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentChannel = null
        _state.value = PlayerState.Idle
    }

    fun release() {
        exoPlayer.release()
        _state.value = PlayerState.Idle
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun setAspectRatio(ratio: AspectRatio) {
        exoPlayer.videoScalingMode = ratio.scalingMode
    }

    // --- Private helpers ---

    private fun retryOrError(cause: PlaybackException) {
        val channel = currentChannel
        if (retryCount < maxRetries && channel != null) {
            retryCount++
            val item = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
        } else {
            _state.value = PlayerState.Error(
                channel = channel,
                message = cause.message ?: "Playback error",
                retryCount = retryCount,
            )
        }
    }

    private fun buildLiveLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs    */ 1_000,
                /* maxBufferMs    */ 6_000,
                /* bufferForPlaybackMs               */ 500,
                /* bufferForPlaybackAfterRebufferMs  */ 1_000,
            )
            .build()

    // --- ExoPlayer listener ---

    private inner class PlayerListener : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            val channel = currentChannel ?: return
            _state.value = when (playbackState) {
                Player.STATE_BUFFERING -> PlayerState.Buffering(channel)
                Player.STATE_READY -> if (exoPlayer.playWhenReady) PlayerState.Playing(channel)
                                      else PlayerState.Paused(channel)
                Player.STATE_ENDED  -> PlayerState.Idle
                Player.STATE_IDLE   -> _state.value // keep current error state if any
                else                -> _state.value
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val channel = currentChannel ?: return
            if (isPlaying) _state.value = PlayerState.Playing(channel)
        }

        override fun onPlayerError(error: PlaybackException) {
            retryOrError(error)
        }
    }
}
