package com.cadnative.firevisioniptv.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.domain.service.ChannelThumbnailExtractor
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

private const val POSITION_SAVE_INTERVAL_MS = 5_000L
private const val DEAD_STREAM_COUNTDOWN_SECONDS = 5
private const val COUNTDOWN_TICK_MS = 1_000L
private const val FINAL_SAVE_TIMEOUT_MS = 3_000L

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannelByIdUseCase: GetChannelByIdUseCase,
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao,
    private val thumbnailExtractor: ChannelThumbnailExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var savePositionJob: Job? = null
    private var loadJob: Job? = null
    private var playbackPositionJob: Job? = null
    private var channelListJob: Job? = null
    private var autoHideJob: Job? = null
    private var countdownJob: Job? = null

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
                        loadPlaybackPosition(channelId)
                        // Preload channel list for next/prev navigation
                        preloadChannelList()
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
                    is Result.Error -> { /* Start from beginning */ }
                }
            }
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, position: Long, duration: Long) {
        _uiState.update {
            it.copy(isPlaying = isPlaying, position = position, duration = duration)
        }
        if (isPlaying) {
            startPeriodicPositionSaving()
        } else {
            stopPeriodicPositionSaving()
            saveCurrentPosition()
        }
    }

    fun updateBufferingState(isBuffering: Boolean) {
        _uiState.update { it.copy(isBuffering = isBuffering) }
    }

    private fun startPeriodicPositionSaving() {
        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            while (true) {
                delay(POSITION_SAVE_INTERVAL_MS)
                saveCurrentPosition()
            }
        }
    }

    private fun stopPeriodicPositionSaving() {
        savePositionJob?.cancel()
        savePositionJob = null
    }

    private fun saveCurrentPosition() {
        val state = _uiState.value
        val channelId = state.channel?.id ?: return
        viewModelScope.launch {
            savePlaybackPositionUseCase(
                SavePlaybackPositionUseCase.Params(
                    channelId = channelId,
                    position = state.position,
                    duration = state.duration
                )
            )
        }
    }

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

    /**
     * Save a thumbnail captured from ExoPlayer for the current channel.
     */
    fun saveThumbnailFromPlayer(bitmap: Bitmap) {
        val channelId = _uiState.value.channel?.id ?: return
        viewModelScope.launch {
            thumbnailExtractor.saveThumbnailBitmap(channelId, bitmap)
        }
    }

    // ── Channel Overlay ─────────────────────────────────────────────

    fun showOverlay() {
        _uiState.update { it.copy(showChannelOverlay = true) }
        // Default to current channel's category
        val currentCategory = _uiState.value.channel?.category
        if (_uiState.value.overlayChannels.isEmpty()) {
            loadChannelList(currentCategory)
        }
        resetAutoHideTimer()
    }

    fun hideOverlay() {
        _uiState.update { it.copy(showChannelOverlay = false) }
        autoHideJob?.cancel()
    }

    /**
     * Preload all channels so next/prev works even before overlay is opened.
     */
    private fun preloadChannelList() {
        if (_uiState.value.overlayChannels.isEmpty()) {
            loadChannelListInternal(null, updateSelection = false)
        }
    }

    fun loadChannelList(category: String? = null) {
        loadChannelListInternal(category, updateSelection = true)
    }

    private fun loadChannelListInternal(category: String?, updateSelection: Boolean) {
        channelListJob?.cancel()
        channelListJob = viewModelScope.launch {
            if (updateSelection) {
                _uiState.update { it.copy(overlayIsLoadingChannels = true, overlaySelectedCategory = category) }
            } else {
                _uiState.update { it.copy(overlayIsLoadingChannels = true) }
            }

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
                        // Always load all categories when loading all channels
                        val categories = if (category == null || _uiState.value.overlayCategories.isEmpty()) {
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

    // ── Next / Previous Channel (D-Pad & remote buttons) ───────────

    fun nextChannel() {
        val channels = _uiState.value.overlayChannels
        if (channels.isEmpty()) return
        val currentId = _uiState.value.channel?.id
        val currentIndex = channels.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex < 0 || currentIndex >= channels.size - 1) 0 else currentIndex + 1
        switchChannel(channels[nextIndex].id)
    }

    fun previousChannel() {
        val channels = _uiState.value.overlayChannels
        if (channels.isEmpty()) return
        val currentId = _uiState.value.channel?.id
        val currentIndex = channels.indexOfFirst { it.id == currentId }
        val prevIndex = if (currentIndex <= 0) channels.size - 1 else currentIndex - 1
        switchChannel(channels[prevIndex].id)
    }

    fun switchChannel(channelId: String) {
        if (channelId == _uiState.value.channel?.id) {
            hideOverlay()
            return
        }
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
            _uiState.update { state ->
                state.copy(
                    overlayChannels = state.overlayChannels.map { ch ->
                        if (ch.id == channelId) ch.copy(isFavorite = !ch.isFavorite) else ch
                    }
                )
            }
            val result = toggleFavoriteUseCase(channelId)
            if (result is Result.Error) {
                _uiState.update { state ->
                    state.copy(
                        overlayChannels = state.overlayChannels.map { ch ->
                            if (ch.id == channelId) ch.copy(isFavorite = !ch.isFavorite) else ch
                        }
                    )
                }
            } else if (channelId == _uiState.value.channel?.id) {
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
            for (seconds in DEAD_STREAM_COUNTDOWN_SECONDS downTo 0) {
                _uiState.update { it.copy(deadStreamCountdown = seconds) }
                if (seconds == 0) {
                    _uiState.update { it.copy(shouldNavigateBack = true) }
                    return@launch
                }
                delay(COUNTDOWN_TICK_MS)
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
        val state = _uiState.value
        val channelId = state.channel?.id ?: return
        val job = SupervisorJob()
        CoroutineScope(Dispatchers.IO + job).launch {
            try {
                kotlinx.coroutines.withTimeout(FINAL_SAVE_TIMEOUT_MS) {
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
