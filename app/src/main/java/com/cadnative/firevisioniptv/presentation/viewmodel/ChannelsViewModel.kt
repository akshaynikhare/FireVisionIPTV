package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.RefreshChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.ChannelsUiState
import com.cadnative.firevisioniptv.presentation.model.PopularCategoryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val channelMapper: ChannelMapper,
    private val channelHealthDao: ChannelHealthDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val playbackPositionDao: PlaybackPositionDao,
    private val favoriteCategoryDao: FavoriteCategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadChannels()
        loadHomeData()
        observeFavoriteCategories()
        refresh()
    }

    fun loadChannels(category: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
                        val uiChannels = channelUiMapper.toUiModelsWithHealth(result.data, healthList)
                        val allCategories = uiChannels
                            .map { it.category }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()

                        // Build category logo collages (up to 4 distinct logos per category)
                        val catLogos = uiChannels
                            .groupBy { it.category }
                            .mapValues { (_, channels) ->
                                channels
                                    .mapNotNull { it.logoUrl }
                                    .distinct()
                                    .take(4)
                            }

                        _uiState.update {
                            it.copy(
                                channels = uiChannels,
                                categories = allCategories,
                                categoryLogos = catLogos,
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
     * Load HomeScreen-specific data: recently watched, featured (most watched),
     * and popular categories. All streams are fully reactive via Room Flows.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun loadHomeData() {
        // Recently watched — auto-updates when user watches a new channel
        viewModelScope.launch {
            try {
                playbackPositionDao.observeRecentlyWatchedIds(20)
                    .flatMapLatest { recentIds ->
                        if (recentIds.isEmpty()) {
                            val empty = emptyList<ChannelUiModel>()
                            flowOf(empty to empty)
                        } else {
                            channelHealthDao.getAllHealth()
                                .debounce(500) // Avoid UI churn during health scans
                                .map { health ->
                                    val recentEntities = channelDao.getChannelsByIds(recentIds)
                                    val favIds = favoriteDao.getFavoriteChannelIds().toSet()
                                    val recentUi = channelUiMapper.toUiModelsWithHealth(
                                        recentEntities.map { channelMapper.toDomain(it, it.id in favIds) },
                                        health
                                    ).filter { it.healthStatus != ChannelHealthStatus.OFFLINE }

                                    // Preserve the order from recentIds
                                    val idOrder = recentIds.withIndex().associate { (i, id) -> id to i }
                                    val sortedRecent = recentUi.sortedBy { idOrder[it.id] ?: Int.MAX_VALUE }
                                    sortedRecent to sortedRecent.take(5)
                                }
                        }
                    }
                    .collect { (recent, featured) ->
                        _uiState.update {
                            it.copy(recentlyWatched = recent, featuredChannels = featured)
                        }
                    }
            } catch (_: Exception) {
                // Non-critical — HomeScreen will just show defaults
            }
        }

        // Popular categories — independent dataset, auto-updates on any change
        viewModelScope.launch {
            try {
                combine(
                    playbackPositionDao.observePopularCategoryIds(10),
                    favoriteCategoryDao.getAllFavoriteCategoryNames(),
                    channelDao.getAllChannels(),
                    channelHealthDao.getAllHealth()
                ) { popularCatIds, favNames, channelEntities, healthList ->
                    val uiChannels = channelUiMapper.toUiModelsWithHealth(
                        channelEntities.map { channelMapper.toDomain(it) },
                        healthList
                    )
                    val categoryChannels = uiChannels.groupBy { it.category }

                    // Build popular categories: favorites first, then by play count
                    val allCatNames = (favNames + popularCatIds).distinct()
                    allCatNames.mapNotNull { catName ->
                        val catChannels = categoryChannels[catName] ?: return@mapNotNull null
                        val liveChannels = catChannels.filter { it.healthStatus != ChannelHealthStatus.OFFLINE }
                        PopularCategoryUiModel(
                            name = catName,
                            channelCount = liveChannels.size,
                            imageUrl = catChannels.firstOrNull { it.thumbnailPath != null }?.thumbnailPath
                                ?: catChannels.firstOrNull { it.logoUrl != null }?.logoUrl,
                            isFavorite = catName in favNames
                        )
                    }
                }
                    .debounce(1000) // Health scans update many rows; batch UI updates
                    .collect { popular ->
                        _uiState.update { it.copy(popularCategories = popular) }
                    }
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }

    private fun observeFavoriteCategories() {
        viewModelScope.launch {
            favoriteCategoryDao.getAllFavoriteCategoryNames().collect { names ->
                _uiState.update { it.copy(favoriteCategoryNames = names.toSet()) }
            }
        }
    }

    fun toggleCategoryFavorite(categoryName: String) {
        viewModelScope.launch {
            val isFav = favoriteCategoryDao.isFavorite(categoryName)
            if (isFav) {
                favoriteCategoryDao.removeFavorite(categoryName)
            } else {
                favoriteCategoryDao.addFavorite(FavoriteCategoryEntity(categoryName = categoryName))
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

            when (result) {
                is Result.Success -> {
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
