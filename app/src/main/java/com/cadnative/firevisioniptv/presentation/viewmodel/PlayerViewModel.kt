package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.usecase.GetChannelByIdUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetPlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.SavePlaybackPositionUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val channelUiMapper: ChannelUiMapper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private var savePositionJob: Job? = null
    
    /**
     * Load a channel for playback.
     * 
     * @param channelId The ID of the channel to load
     */
    fun loadChannel(channelId: String) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
     * Toggle controls visibility.
     */
    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }
    
    /**
     * Show controls.
     */
    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
    }
    
    /**
     * Hide controls.
     */
    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
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
     * Handle playback error.
     * 
     * @param error The error message
     */
    fun onPlaybackError(error: String) {
        _uiState.update { it.copy(error = error, isPlaying = false) }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Save position one last time before clearing
        saveCurrentPosition()
        stopPeriodicPositionSaving()
    }
}
