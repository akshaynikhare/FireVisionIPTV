package com.cabletech.iptvplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cabletech.iptvplayer.core.model.Channel

/**
 * Thin wrapper around [ExoPlayer] / Media3 for IPTV stream playback.
 *
 * Supports HLS, DASH, RTSP, and plain HTTP progressive streams.
 * Lifecycle management (release/pause) is the caller's responsibility.
 */
class IptvPlayer(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    fun play(channel: Channel) {
        val mediaItem = MediaItem.fromUri(channel.streamUrl)
        exoPlayer.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun resume() {
        exoPlayer.playWhenReady = true
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun release() {
        exoPlayer.release()
    }

    val isPlaying: Boolean get() = exoPlayer.isPlaying

    val playbackState: Int get() = exoPlayer.playbackState

    fun addListener(listener: Player.Listener) = exoPlayer.addListener(listener)
    fun removeListener(listener: Player.Listener) = exoPlayer.removeListener(listener)
}
