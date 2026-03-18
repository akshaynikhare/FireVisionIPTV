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
 *
 * When a network error occurs, retries up to [maxReconnectAttempts] times
 * with exponential backoff. Non-recoverable errors (bad manifest, server
 * error) immediately signal the stream as dead.
 */
class ErrorRecoveryManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val onError: (String) -> Unit,
    private val onRecovering: (attempt: Int) -> Unit,
    private val onRecovered: () -> Unit,
    private val onStreamDead: (errorMessage: String) -> Unit,
    private val onStreamUnresponsive: (() -> Unit)? = null
) {
    private var reconnectJob: Job? = null
    private var bufferWatchJob: Job? = null
    private var reconnectAttempts = 0
    private var isRecoveringState = false
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L
    private val unresponsiveThresholdMs = 30_000L

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && isRecoveringState) {
                reconnectAttempts = 0
                isRecoveringState = false
                reconnectJob?.cancel()
                onRecovered()
            }

            // Track buffering for unresponsive detection
            // Skip during error recovery — recovery already handles retries
            if (playbackState == Player.STATE_BUFFERING && !isRecoveringState) {
                startBufferWatch()
            } else {
                bufferWatchJob?.cancel()
                bufferWatchJob = null
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    private fun startBufferWatch() {
        bufferWatchJob?.cancel()
        bufferWatchJob = scope.launch {
            delay(unresponsiveThresholdMs)
            // Still buffering after threshold — stream is unresponsive
            if (player.playbackState == Player.STATE_BUFFERING) {
                onStreamUnresponsive?.invoke()
            }
        }
    }

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

        if (isNetworkError(error) && reconnectAttempts < maxReconnectAttempts) {
            onError(errorMessage)
            attemptReconnect()
        } else {
            // Non-recoverable or retries exhausted
            onStreamDead(errorMessage)
        }
    }

    private fun isNetworkError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
    }

    private fun attemptReconnect() {
        reconnectJob?.cancel()
        isRecoveringState = true
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayTime = reconnectDelayMs * reconnectAttempts

            onRecovering(reconnectAttempts)
            delay(delayTime)

            player.prepare()
            player.play()
        }
    }

    /**
     * Reset reconnection state. Call when switching to a new channel.
     */
    fun reset() {
        reconnectAttempts = 0
        isRecoveringState = false
        reconnectJob?.cancel()
        bufferWatchJob?.cancel()
    }

    fun retry() {
        reconnectAttempts = 0
        isRecoveringState = true
        player.prepare()
        player.play()
    }

    fun release() {
        reconnectJob?.cancel()
        bufferWatchJob?.cancel()
        player.removeListener(playerListener)
    }
}
