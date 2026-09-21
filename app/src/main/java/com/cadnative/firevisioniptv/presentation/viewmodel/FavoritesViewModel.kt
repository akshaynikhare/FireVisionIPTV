package com.cadnative.firevisioniptv.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.R
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.domain.usecase.GetFavoriteChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReorderFavoritesUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.FavoritesUiState
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val reorderFavoritesUseCase: ReorderFavoritesUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao,
    private val channelDao: ChannelDao,
    private val favoriteCategoryDao: FavoriteCategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val reorderMutex = Mutex()
    private var loadJob: Job? = null
    private var categoriesJob: Job? = null

    init {
        loadFavorites()
        loadFavoriteCategories()
    }

    fun retryLoadFavorites() {
        loadFavorites()
        loadFavoriteCategories()
    }

    private fun loadFavoriteCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            combine(
                favoriteCategoryDao.getAllFavoriteCategories(),
                channelDao.getAllChannels(),
                channelHealthDao.getAllHealth()
            ) { favCategories, allChannels, healthList ->
                val channelsByCategory = allChannels.groupBy { it.categoryId }
                val healthMap = healthList.associateBy { it.channelId }
                favCategories.mapNotNull { favCat ->
                    val catChannels = channelsByCategory[favCat.categoryName] ?: return@mapNotNull null
                    PopularCategoryUiModel(
                        name = favCat.categoryName,
                        channelCount = catChannels.size,
                        imageUrl = catChannels.firstNotNullOfOrNull { ch ->
                            healthMap[ch.id]?.thumbnailPath
                        } ?: catChannels.firstNotNullOfOrNull { it.logoUrl },
                        isFavorite = true
                    )
                }
            }.collect { categories ->
                _uiState.update { it.copy(favoriteCategories = categories) }
            }
        }
    }

    fun removeFavoriteCategory(categoryName: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    favoriteCategories = state.favoriteCategories.filter { it.name != categoryName }
                )
            }
            favoriteCategoryDao.removeFavorite(categoryName)
        }
    }

    private fun loadFavorites() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
                                    error = result.exception.message
                                        ?: context.getString(R.string.error_load_favorites)
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
                        error = result.exception.message
                            ?: context.getString(R.string.error_remove_favorite)
                    )
                }
                loadFavorites()
            }
        }
    }

    fun reorderFavorite(channelId: String, newPosition: Int) {
        viewModelScope.launch {
            reorderMutex.withLock {
                performReorder(channelId, newPosition)
            }
        }
    }

    private suspend fun performReorder(channelId: String, newPosition: Int) {
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
                        error = result.exception.message
                            ?: context.getString(R.string.error_reorder_favorites)
                    )
                }
                loadFavorites()
            }
        }
    }

    fun moveFavoriteUp(channelId: String) {
        viewModelScope.launch {
            reorderMutex.withLock {
                val currentIndex = _uiState.value.favorites.indexOfFirst { it.id == channelId }
                if (currentIndex > 0) {
                    performReorder(channelId, currentIndex - 1)
                }
            }
        }
    }

    fun moveFavoriteDown(channelId: String) {
        viewModelScope.launch {
            reorderMutex.withLock {
                val favorites = _uiState.value.favorites
                val currentIndex = favorites.indexOfFirst { it.id == channelId }
                if (currentIndex != -1 && currentIndex < favorites.size - 1) {
                    performReorder(channelId, currentIndex + 1)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
