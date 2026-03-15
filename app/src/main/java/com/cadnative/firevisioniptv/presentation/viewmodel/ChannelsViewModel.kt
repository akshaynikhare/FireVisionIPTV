package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.ChannelsUiState
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val refreshChannelsUseCase: RefreshChannelsUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
        refresh()
    }

    fun loadChannels(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

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
                        Log.d("ChannelsVM", "Got ${result.data.size} channels from flow")
                        val uiChannels = channelUiMapper.toUiModelsWithHealth(result.data, healthList)
                        val allCategories = uiChannels
                            .map { it.category }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
                        _uiState.update {
                            it.copy(
                                channels = uiChannels,
                                categories = allCategories,
                                isLoading = false,
                                selectedCategory = category
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e("ChannelsVM", "Error loading channels: ${result.exception.message}")
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

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
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

            val result = toggleFavoriteUseCase(channelId)

            if (result is Result.Error) {
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = refreshChannelsUseCase(Unit)
            Log.d("ChannelsVM", "Refresh result: $result")

            when (result) {
                is Result.Success -> {
                    Log.d("ChannelsVM", "Refresh success - channels should update via Flow")
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    Log.e("ChannelsVM", "Refresh error: ${result.exception.message}")
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
