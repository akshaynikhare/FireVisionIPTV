package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.usecase.GetFavoriteChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReorderFavoritesUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.FavoritesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val reorderFavoritesUseCase: ReorderFavoritesUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getFavoriteChannelsUseCase(Unit)
                .combine(channelHealthDao.getAllHealth()) { result, healthList ->
                    result to healthList
                }
                .collect { (result, healthList) ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    favorites = channelUiMapper.toUiModelsWithHealth(result.data, healthList),
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

    fun removeFavorite(channelId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    favorites = state.favorites.filter { it.id != channelId }
                )
            }

            val result = toggleFavoriteUseCase(channelId)

            if (result is Result.Error) {
                _uiState.update {
                    it.copy(
                        error = result.exception.message ?: "Failed to remove favorite"
                    )
                }
                loadFavorites()
            }
        }
    }

    fun reorderFavorite(channelId: String, newPosition: Int) {
        viewModelScope.launch {
            val currentFavorites = _uiState.value.favorites
            val currentIndex = currentFavorites.indexOfFirst { it.id == channelId }

            if (currentIndex != -1 && newPosition in currentFavorites.indices) {
                val mutableList = currentFavorites.toMutableList()
                val item = mutableList.removeAt(currentIndex)
                mutableList.add(newPosition, item)

                _uiState.update { it.copy(favorites = mutableList) }

                val params = ReorderFavoritesUseCase.Params(
                    channelId = channelId,
                    newOrder = newPosition
                )

                val result = reorderFavoritesUseCase(params)

                if (result is Result.Error) {
                    _uiState.update {
                        it.copy(
                            error = result.exception.message ?: "Failed to reorder favorites"
                        )
                    }
                    loadFavorites()
                }
            }
        }
    }

    fun moveFavoriteUp(channelId: String) {
        val currentIndex = _uiState.value.favorites.indexOfFirst { it.id == channelId }
        if (currentIndex > 0) {
            reorderFavorite(channelId, currentIndex - 1)
        }
    }

    fun moveFavoriteDown(channelId: String) {
        val favorites = _uiState.value.favorites
        val currentIndex = favorites.indexOfFirst { it.id == channelId }
        if (currentIndex != -1 && currentIndex < favorites.size - 1) {
            reorderFavorite(channelId, currentIndex + 1)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
