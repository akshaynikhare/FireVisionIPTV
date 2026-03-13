package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.domain.usecase.GetFavoriteChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReorderFavoritesUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.FavoritesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the favorites screen.
 * 
 * Manages favorite channels, including reordering and quick removal.
 * 
 * Requirements: US-008 (Enhanced Favorites)
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val reorderFavoritesUseCase: ReorderFavoritesUseCase,
    private val channelUiMapper: ChannelUiMapper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    
    init {
        loadFavorites()
    }
    
    /**
     * Load favorite channels.
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getFavoriteChannelsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                favorites = result.data.map { channel ->
                                    channelUiMapper.toUiModel(channel)
                                },
                                isLoading = false
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load favorites"
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Remove a channel from favorites.
     * 
     * @param channelId The ID of the channel to remove
     */
    fun removeFavorite(channelId: String) {
        viewModelScope.launch {
            // Optimistic UI update
            _uiState.update { state ->
                state.copy(
                    favorites = state.favorites.filter { it.id != channelId }
                )
            }
            
            // Perform the actual operation
            val result = toggleFavoriteUseCase(channelId)
            
            // Handle errors
            if (result is Result.Error) {
                _uiState.update {
                    it.copy(
                        error = result.exception.message ?: "Failed to remove favorite"
                    )
                }
                // Reload to restore correct state
                loadFavorites()
            }
        }
    }
    
    /**
     * Reorder favorites by moving a channel to a new position.
     * 
     * @param channelId The ID of the channel to move
     * @param newPosition The new position (0-based index)
     */
    fun reorderFavorite(channelId: String, newPosition: Int) {
        viewModelScope.launch {
            // Optimistic UI update
            val currentFavorites = _uiState.value.favorites
            val currentIndex = currentFavorites.indexOfFirst { it.id == channelId }
            
            if (currentIndex != -1 && newPosition in currentFavorites.indices) {
                val mutableList = currentFavorites.toMutableList()
                val item = mutableList.removeAt(currentIndex)
                mutableList.add(newPosition, item)
                
                _uiState.update { it.copy(favorites = mutableList) }
                
                // Perform the actual operation
                val params = ReorderFavoritesUseCase.Params(
                    channelId = channelId,
                    newOrder = newPosition
                )
                
                val result = reorderFavoritesUseCase(params)
                
                // Handle errors
                if (result is Result.Error) {
                    _uiState.update {
                        it.copy(
                            error = result.exception.message ?: "Failed to reorder favorites"
                        )
                    }
                    // Reload to restore correct state
                    loadFavorites()
                }
            }
        }
    }
    
    /**
     * Move a favorite up in the list.
     * 
     * @param channelId The ID of the channel to move up
     */
    fun moveFavoriteUp(channelId: String) {
        val currentIndex = _uiState.value.favorites.indexOfFirst { it.id == channelId }
        if (currentIndex > 0) {
            reorderFavorite(channelId, currentIndex - 1)
        }
    }
    
    /**
     * Move a favorite down in the list.
     * 
     * @param channelId The ID of the channel to move down
     */
    fun moveFavoriteDown(channelId: String) {
        val favorites = _uiState.value.favorites
        val currentIndex = favorites.indexOfFirst { it.id == channelId }
        if (currentIndex != -1 && currentIndex < favorites.size - 1) {
            reorderFavorite(channelId, currentIndex + 1)
        }
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
