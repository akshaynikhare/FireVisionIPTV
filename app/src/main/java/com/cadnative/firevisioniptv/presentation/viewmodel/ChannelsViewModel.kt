package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.ChannelsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the channels screen.
 * 
 * Manages the UI state for displaying channels, handling user interactions
 * like toggling favorites, and refreshing channel data from the server.
 * 
 * Requirements: TR-003 (MVVM pattern with ViewModels)
 */
@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val refreshChannelsUseCase: RefreshChannelsUseCase,
    private val channelUiMapper: ChannelUiMapper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()
    
    init {
        loadChannels()
    }
    
    /**
     * Load channels, optionally filtered by category.
     * 
     * @param category Optional category to filter by, null for all channels
     */
    fun loadChannels(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val flow = if (category != null) {
                getChannelsByCategoryUseCase(category)
            } else {
                getChannelsUseCase(Unit)
            }
            
            flow.collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                channels = result.data.map { channel ->
                                    channelUiMapper.toUiModel(channel)
                                },
                                isLoading = false,
                                selectedCategory = category
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Unknown error"
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Toggle the favorite status of a channel.
     * 
     * Updates the UI optimistically before the operation completes.
     * 
     * @param channelId The ID of the channel to toggle
     */
    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            // Optimistic UI update
            _uiState.update { state ->
                state.copy(
                    channels = state.channels.map { channel ->
                        if (channel.id == channelId) {
                            channel.copy(isFavorite = !channel.isFavorite)
                        } else {
                            channel
                        }
                    }
                )
            }
            
            // Perform the actual operation
            val result = toggleFavoriteUseCase(channelId)
            
            // Handle errors (success is already reflected in UI)
            if (result is Result.Error) {
                // Revert optimistic update on error
                _uiState.update { state ->
                    state.copy(
                        channels = state.channels.map { channel ->
                            if (channel.id == channelId) {
                                channel.copy(isFavorite = !channel.isFavorite)
                            } else {
                                channel
                            }
                        },
                        error = result.exception.message ?: "Failed to update favorite"
                    )
                }
            }
        }
    }
    
    /**
     * Refresh channels from the server.
     * 
     * Triggers a background refresh while keeping the current data visible.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = refreshChannelsUseCase(Unit)
            
            when (result) {
                is Result.Success -> {
                    // Channels will be updated automatically via Flow
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to refresh"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
