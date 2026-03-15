package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.domain.usecase.GetChannelByIdUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetPlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.SavePlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.AUTO_HIDE_DELAY_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the player screen.
 * 
 * Manages playback state, channel information, and periodically saves
 * playback position for resume functionality.
 * 
 * Requirements: US-004 (Better Video Playback)
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannelByIdUseCase: GetChannelByIdUseCase,
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var savePositionJob: Job? = null
    private var loadJob: Job? = null
    private var playbackPositionJob: Job? = null
    private var channelListJob: Job? = null
    private var autoHideJob: Job? = null
    private var countdownJob: Job? = null

    /**
     * Load a channel for playback.
     *
     * @param channelId The ID of the channel to load
     */
    fun loadChannel(channelId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getChannelByIdUseCase(channelId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                channel = channelUiMapper.toUiModel(result.data),
                                isLoading = false,
                                error = null
                            )
                        }
                        
                        // Load saved playback position
                        loadPlaybackPosition(channelId)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load channel"
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Load the saved playback position for the current channel.
     */
    private fun loadPlaybackPosition(channelId: String) {
        playbackPositionJob?.cancel()
        playbackPositionJob = viewModelScope.launch {
            getPlaybackPositionUseCase(channelId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        result.data?.let { playbackState ->
                            _uiState.update {
                                it.copy(
                                    position = playbackState.position,
                                    duration = playbackState.duration
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        // Silently fail - start from beginning
                    }
                }
            }
        }
    }
    
    /**
     * Update playback state.
     * 
     * @param isPlaying Whether playback is active
     * @param position Current playback position in milliseconds
     * @param duration Total duration in milliseconds
     */
    fun updatePlaybackState(isPlaying: Boolean, position: Long, duration: Long) {
        _uiState.update {
            it.copy(
                isPlaying = isPlaying,
                position = position,
                duration = duration
            )
        }
        
        // Start periodic position saving when playing
        if (isPlaying) {
            startPeriodicPositionSaving()
        } else {
            stopPeriodicPositionSaving()
            // Save position immediately when paused
            saveCurrentPosition()
        }
    }
    
    /**
     * Update buffering state.
     * 
     * @param isBuffering Whether the player is currently buffering
     */
    fun updateBufferingState(isBuffering: Boolean) {
        _uiState.update { it.copy(isBuffering = isBuffering) }
    }
    
    /**
     * Start periodically saving playback position.
     * 
     * Saves position every 5 seconds during playback.
     */
    private fun startPeriodicPositionSaving() {
        // Cancel existing job if any
        savePositionJob?.cancel()
        
        savePositionJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Save every 5 seconds
                saveCurrentPosition()
            }
        }
    }
    
    /**
     * Stop periodic position saving.
     */
    private fun stopPeriodicPositionSaving() {
        savePositionJob?.cancel()
        savePositionJob = null
    }
    
    /**
     * Save the current playback position.
     */
    private fun saveCurrentPosition() {
        val state = _uiState.value
        val channelId = state.channel?.id ?: return
        
        viewModelScope.launch {
            val params = SavePlaybackPositionUseCase.Params(
                channelId = channelId,
                position = state.position,
                duration = state.duration
            )
            
            savePlaybackPositionUseCase(params)
            // Silently fail - not critical
        }
    }
    
    /**
     * Toggle favorite status for the current channel.
     */
    fun toggleFavorite() {
        val channelId = _uiState.value.channel?.id ?: return
        viewModelScope.launch {
            val result = toggleFavoriteUseCase(channelId)
            if (result is Result.Success) {
                _uiState.update { state ->
                    state.channel?.let { channel ->
                        state.copy(channel = channel.copy(isFavorite = !channel.isFavorite))
                    } ?: state
                }
            }
        }
    }

    // ── Channel Overlay ─────────────────────────────────────────────

    fun showOverlay() {
        _uiState.update { it.copy(showChannelOverlay = true) }
        if (_uiState.value.overlayChannels.isEmpty()) {
            loadChannelList()
        }
        resetAutoHideTimer()
    }

    fun hideOverlay() {
        _uiState.update { it.copy(showChannelOverlay = false) }
        autoHideJob?.cancel()
    }

    fun loadChannelList(category: String? = null) {
        channelListJob?.cancel()
        channelListJob = viewModelScope.launch {
            _uiState.update { it.copy(overlayIsLoadingChannels = true, overlaySelectedCategory = category) }

            val channelFlow = if (category != null) {
                getChannelsByCategoryUseCase(category)
            } else {
                getChannelsUseCase(Unit)
            }

            channelFlow.combine(channelHealthDao.getAllHealth()) { result, healthList ->
                result to healthList
            }.collect { (result, healthList) ->
                when (result) {
                    is Result.Success -> {
                        val uiChannels = channelUiMapper.toUiModelsWithHealth(result.data, healthList)
                        val categories = if (category == null) {
                            uiChannels.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                        } else {
                            _uiState.value.overlayCategories
                        }
                        _uiState.update {
                            it.copy(
                                overlayChannels = uiChannels,
                                overlayCategories = categories,
                                overlayIsLoadingChannels = false
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(overlayIsLoadingChannels = false) }
                    }
                }
            }
        }
    }

    fun switchChannel(channelId: String) {
        if (channelId == _uiState.value.channel?.id) {
            hideOverlay()
            return
        }
        // Reset recovery/dead-stream state for the new channel
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                isRecovering = false,
                isStreamDead = false,
                deadStreamCountdown = 0,
                shouldNavigateBack = false,
                error = null
            )
        }
        saveCurrentPosition()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isSwitchingChannel = true) }
            getChannelByIdUseCase(channelId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                channel = channelUiMapper.toUiModel(result.data),
                                isSwitchingChannel = false,
                                showChannelOverlay = false,
                                position = 0L,
                                duration = 0L
                            )
                        }
                        loadPlaybackPosition(channelId)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isSwitchingChannel = false,
                                error = result.exception.message ?: "Failed to switch channel"
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch {
            delay(AUTO_HIDE_DELAY_MS)
            _uiState.update { it.copy(showChannelOverlay = false) }
        }
    }

    fun toggleOverlayFavorite(channelId: String) {
        viewModelScope.launch {
            // Optimistic update on overlay list
            _uiState.update { state ->
                state.copy(
                    overlayChannels = state.overlayChannels.map { ch ->
                        if (ch.id == channelId) ch.copy(isFavorite = !ch.isFavorite) else ch
                    }
                )
            }
            val result = toggleFavoriteUseCase(channelId)
            if (result is Result.Error) {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        overlayChannels = state.overlayChannels.map { ch ->
                            if (ch.id == channelId) ch.copy(isFavorite = !ch.isFavorite) else ch
                        }
                    )
                }
            } else if (channelId == _uiState.value.channel?.id) {
                // Also update current channel if same (only on success)
                _uiState.update { state ->
                    state.channel?.let { ch ->
                        state.copy(channel = ch.copy(isFavorite = !ch.isFavorite))
                    } ?: state
                }
            }
        }
    }

    // ── Stream Recovery & Dead-Stream Handling ─────────────────────

    fun onPlaybackError(error: String) {
        _uiState.update { it.copy(error = error, isPlaying = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onRecovering(attempt: Int) {
        _uiState.update {
            it.copy(
                isRecovering = true,
                recoveryAttempt = attempt,
                error = null,
                isStreamDead = false
            )
        }
    }

    fun onRecovered() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                isRecovering = false,
                recoveryAttempt = 0,
                error = null,
                isStreamDead = false,
                deadStreamCountdown = 0
            )
        }
    }

    fun onStreamDead(errorMessage: String) {
        val channelId = _uiState.value.channel?.id ?: return

        _uiState.update {
            it.copy(
                isRecovering = false,
                isPlaying = false,
                isStreamDead = true,
                deadStreamMessage = "Stream unavailable",
                error = null
            )
        }

        // Mark channel OFFLINE in health database
        viewModelScope.launch {
            channelHealthDao.upsertPreservingThumbnail(
                channelId = channelId,
                status = ChannelHealthStatus.OFFLINE.name,
                lastCheckedAt = System.currentTimeMillis(),
                responseTimeMs = null,
                errorMessage = errorMessage
            )
        }

        startDeadStreamCountdown()
    }

    private fun startDeadStreamCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in 5 downTo 0) {
                _uiState.update { it.copy(deadStreamCountdown = seconds) }
                if (seconds == 0) {
                    _uiState.update { it.copy(shouldNavigateBack = true) }
                    return@launch
                }
                delay(1000L)
            }
        }
    }

    fun cancelDeadStreamCountdown() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(deadStreamCountdown = 0, shouldNavigateBack = false)
        }
    }

    fun onNavigatedBack() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPeriodicPositionSaving()
        playbackPositionJob?.cancel()
        channelListJob?.cancel()
        autoHideJob?.cancel()
        countdownJob?.cancel()
        // viewModelScope is already cancelled at this point, so use a dedicated scope
        // for the final position save. Use withTimeout to prevent leaking.
        val state = _uiState.value
        val channelId = state.channel?.id ?: return
        val job = SupervisorJob()
        CoroutineScope(Dispatchers.IO + job).launch {
            try {
                kotlinx.coroutines.withTimeout(3000L) {
                    savePlaybackPositionUseCase(
                        SavePlaybackPositionUseCase.Params(
                            channelId = channelId,
                            position = state.position,
                            duration = state.duration
                        )
                    )
                }
            } finally {
                job.cancel()
            }
        }
    }
}
