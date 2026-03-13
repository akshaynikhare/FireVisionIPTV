package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages error recovery for playback with automatic reconnection.
 */
class ErrorRecoveryManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val onError: (String) -> Unit,
    private val onRecovering: () -> Unit,
    private val onRecovered: () -> Unit
) {
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    // Playback recovered
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    onRecovered()
                }
                Player.STATE_BUFFERING -> {
                    // Show buffering indicator
                }
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    /**
     * Handle playback errors with automatic recovery.
     */
    private fun handleError(error: PlaybackException) {
        val errorMessage = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                "Network connection failed"
            }
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                "Server error"
            }
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                "Invalid stream format"
            }
            else -> {
                "Playback error: ${error.message}"
            }
        }

        onError(errorMessage)

        // Attempt automatic reconnection for network errors
        if (isNetworkError(error) && reconnectAttempts < maxReconnectAttempts) {
            attemptReconnect()
        }
    }

    /**
     * Check if error is network-related.
     */
    private fun isNetworkError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    /**
     * Attempt to reconnect with exponential backoff.
     */
    private fun attemptReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayTime = reconnectDelayMs * reconnectAttempts
            
            onRecovering()
            delay(delayTime)

            // Retry playback
            player.prepare()
            player.play()
        }
    }

    /**
     * Manually retry playback.
     */
    fun retry() {
        reconnectAttempts = 0
        player.prepare()
        player.play()
    }

    /**
     * Release resources.
     */
    fun release() {
        reconnectJob?.cancel()
        player.removeListener(playerListener)
    }
}
