package com.cadnative.firevisioniptv.presentation.ui.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages error recovery for playback with automatic reconnection,
 * proxy fallback, and alternate stream fallback.
 *
 * Recovery strategy:
 * 1. Primary direct URL: up to 2 attempts with exponential backoff
 * 2. Primary proxy URL: 1 attempt (if proxy available)
 * 3. For each alternate (up to 3): 1 direct attempt, then 1 proxy attempt
 * 4. If all retries exhausted, signal the stream as dead
 */
class ErrorRecoveryManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val onError: (String) -> Unit,
    private val onRecovering: (attempt: Int) -> Unit,
    private val onRecovered: () -> Unit,
    private val onStreamDead: (errorMessage: String) -> Unit,
    private val onStreamUnresponsive: (() -> Unit)? = null,
    private val onProxyFallback: (() -> Unit)? = null,
    private val onAlternateFallback: ((streamUrl: String) -> Unit)? = null
) {
    data class StreamSlot(
        val directUrl: String,
        val proxyUrl: String?,
        val isPrimary: Boolean
    )

    private var reconnectJob: Job? = null
    private var bufferWatchJob: Job? = null
    private var isRecoveringState = false

    private var streamSlots: List<StreamSlot> = emptyList()
    private var currentSlotIndex = 0
    private var attemptInSlot = 0
    private var totalAttempts = 0

    private val reconnectDelayMs = 2000L
    private val unresponsiveThresholdMs = 30_000L

    /** The stream URL that is currently being played (direct or proxy). */
    val activeStreamUrl: String?
        get() {
            val slot = streamSlots.getOrNull(currentSlotIndex) ?: return null
            return slot.directUrl
        }

    val maxTotalAttempts: Int
        get() = streamSlots.sumOf { maxAttemptsForSlot(streamSlots.indexOf(it)) }

    private fun maxAttemptsForSlot(slotIndex: Int): Int {
        val slot = streamSlots.getOrNull(slotIndex) ?: return 0
        val directAttempts = if (slotIndex == 0) 2 else 1
        val proxyAttempts = if (slot.proxyUrl != null) 1 else 0
        return directAttempts + proxyAttempts
    }

    private fun isProxyAttempt(): Boolean {
        val slot = streamSlots.getOrNull(currentSlotIndex) ?: return false
        val directAttempts = if (currentSlotIndex == 0) 2 else 1
        return attemptInSlot > directAttempts && slot.proxyUrl != null
    }

    private fun currentUrl(): String? {
        val slot = streamSlots.getOrNull(currentSlotIndex) ?: return null
        return if (isProxyAttempt()) slot.proxyUrl else slot.directUrl
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && isRecoveringState) {
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
            if (player.playbackState == Player.STATE_BUFFERING) {
                onStreamUnresponsive?.invoke()
            }
        }
    }

    /**
     * Set the stream slots to use for fallback. The first slot is the primary stream.
     * Replaces the old setProxyUrl() API.
     */
    fun setStreamSlots(slots: List<StreamSlot>) {
        streamSlots = slots
        currentSlotIndex = 0
        attemptInSlot = 0
        totalAttempts = 0
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

        if (isNetworkError(error) && totalAttempts < maxTotalAttempts) {
            onError(errorMessage)
            attemptReconnect()
        } else {
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
            attemptInSlot++
            totalAttempts++

            // Check if we've exhausted attempts for the current slot
            val maxForSlot = maxAttemptsForSlot(currentSlotIndex)
            if (attemptInSlot > maxForSlot) {
                // Move to next slot
                currentSlotIndex++
                attemptInSlot = 1

                if (currentSlotIndex >= streamSlots.size) {
                    onStreamDead("All streams exhausted")
                    return@launch
                }

                val newSlot = streamSlots[currentSlotIndex]
                onAlternateFallback?.invoke(newSlot.directUrl)

                val mediaItem = MediaItem.Builder()
                    .setUri(newSlot.directUrl)
                    .build()
                player.setMediaItem(mediaItem)
            } else if (isProxyAttempt()) {
                // Switch to proxy for current slot
                val proxyUrl = streamSlots[currentSlotIndex].proxyUrl!!
                onProxyFallback?.invoke()
                val mediaItem = MediaItem.Builder()
                    .setUri(proxyUrl)
                    .build()
                player.setMediaItem(mediaItem)
            }

            val delayTime = reconnectDelayMs * attemptInSlot
            onRecovering(totalAttempts)
            delay(delayTime)

            player.prepare()
            player.play()
        }
    }

    /**
     * Reset reconnection state. Call when switching to a new channel.
     */
    fun reset() {
        currentSlotIndex = 0
        attemptInSlot = 0
        totalAttempts = 0
        isRecoveringState = false
        reconnectJob?.cancel()
        bufferWatchJob?.cancel()
    }

    fun retry() {
        currentSlotIndex = 0
        attemptInSlot = 0
        totalAttempts = 0
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
