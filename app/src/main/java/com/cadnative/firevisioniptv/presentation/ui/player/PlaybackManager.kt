package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages playback features including position saving and restoration.
 */
class PlaybackManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val onSavePosition: (Long, Long) -> Unit
) {
    private var positionSaveJob: Job? = null

    /**
     * Start periodic position saving (every 5 seconds).
     */
    fun startPositionSaving() {
        positionSaveJob?.cancel()
        positionSaveJob = scope.launch {
            while (isActive) {
                delay(5000) // Save every 5 seconds
                if (player.isPlaying) {
                    val position = player.currentPosition
                    val duration = player.duration
                    if (duration > 0) {
                        onSavePosition(position, duration)
                    }
                }
            }
        }
    }

    /**
     * Stop position saving.
     */
    fun stopPositionSaving() {
        positionSaveJob?.cancel()
        positionSaveJob = null
    }

    /**
     * Restore playback position.
     */
    fun restorePosition(position: Long) {
        if (position > 0 && player.duration > 0) {
            player.seekTo(position)
        }
    }

    /**
     * Handle channel switching with smooth transition.
     */
    fun switchChannel(streamUrl: String, savedPosition: Long = 0) {
        player.stop()
        player.clearMediaItems()
        
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(streamUrl)
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        
        if (savedPosition > 0) {
            player.seekTo(savedPosition)
        }
        
        player.play()
    }

    /**
     * Configure adaptive bitrate streaming.
     */
    fun configureAdaptiveBitrate() {
        // ExoPlayer handles adaptive bitrate automatically
        // Additional configuration can be added here if needed
    }

    /**
     * Release resources.
     */
    fun release() {
        stopPositionSaving()
    }
}
