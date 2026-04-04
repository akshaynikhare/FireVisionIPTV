package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.mapper.ChannelMapper
import com.cadnative.firevisioniptv.data.source.remote.NetworkException
import com.cadnative.firevisioniptv.data.source.remote.ServerException
import com.cadnative.firevisioniptv.data.source.remote.ServiceUnavailableException
import com.cadnative.firevisioniptv.data.source.remote.UnauthorizedException
import com.cadnative.firevisioniptv.data.source.remote.ForbiddenException
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteCategoryDao
import com.cadnative.firevisioniptv.data.source.local.dao.FavoriteDao
import com.cadnative.firevisioniptv.data.source.local.dao.PlaybackPositionDao
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RECENTLY_WATCHED_LIMIT = 20
private const val FEATURED_CHANNELS_LIMIT = 5
private const val POPULAR_CATEGORIES_LIMIT = 10
private const val HEALTH_SCAN_DEBOUNCE_MS = 500L
private const val CATEGORY_UPDATE_DEBOUNCE_MS = 1_000L

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val refreshChannelsUseCase: RefreshChannelsUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelMapper: ChannelMapper,
    private val epgRepository: EpgRepository,
    private val channelHealthDao: ChannelHealthDao,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val playbackPositionDao: PlaybackPositionDao,
    private val favoriteCategoryDao: FavoriteCategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null
    private var recentlyWatchedJob: Job? = null
    private var popularCategoriesJob: Job? = null
    private var hasResumedBefore = false

    init {
        viewModelScope.launch {
            epgRepository.ensureLoaded()
            // Re-enrich displayed channels once EPG is loaded
            _uiState.update { state ->
                state.copy(
                    channels = state.channels.map { enrichWithEpgIfReady(it) },
                    recentlyWatched = state.recentlyWatched.map { enrichWithEpgIfReady(it) },
                    featuredChannels = state.featuredChannels.map { enrichWithEpgIfReady(it) }
                )
            }
        }
        loadChannels()
        loadHomeData()
        observeFavoriteCategories()
        refresh()
    }

    @OptIn(FlowPreview::class)
    fun loadChannels(category: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isUserCategorySwitch = category != null && category != _uiState.value.selectedCategory
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = if (isUserCategorySwitch) null else it.error,
                    errorType = if (isUserCategorySwitch) ErrorType.NONE else it.errorType
                )
            }

            // If entering with a category filter but categories aren't loaded yet
            // (e.g. navigated directly via ChannelsByCategory), bootstrap them from Room.
            if (category != null && _uiState.value.categories.isEmpty()) {
                val allEntities = channelDao.getAllActiveChannels()
                if (allEntities.isNotEmpty()) {
                    val cats = allEntities.map { it.categoryId }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    _uiState.update { state -> state.copy(categories = cats) }
                }
            }

            val channelFlow = if (category != null) {
                getChannelsByCategoryUseCase(category)
            } else {
                getChannelsUseCase(Unit)
            }

            channelFlow.combine(
                channelHealthDao.getAllHealth()
                    .debounce(HEALTH_SCAN_DEBOUNCE_MS)
                    .onStart { emit(emptyList()) }
            ) { result, healthList ->
                result to healthList
            }.collect { (result, healthList) ->
                when (result) {
                    is Result.Success -> {
                        val uiChannels = channelUiMapper.toUiModelsWithHealth(result.data, healthList)
                            .map { enrichWithEpgIfReady(it) }

                        // Only rebuild categories/logos when showing all channels (no filter).
                        // When a category is selected we show a filtered subset, but the full
                        // category list must stay visible so the user can jump between categories.
                        val allCategories: List<String>
                        val catLogos: Map<String, List<String>>
                        if (category == null) {
                            allCategories = uiChannels
                                .map { it.category }
                                .filter { it.isNotBlank() }
                                .distinct()
                                .sorted()
                            catLogos = uiChannels
                                .groupBy { it.category }
                                .mapValues { (_, channels) ->
                                    channels
                                        .mapNotNull { it.logoUrl }
                                        .distinct()
                                        .take(4)
                                }
                        } else {
                            // Keep existing categories & logos from the previous "all" load
                            allCategories = _uiState.value.categories
                            catLogos = _uiState.value.categoryLogos
                        }

                        _uiState.update {
                            it.copy(
                                channels = uiChannels,
                                categories = allCategories,
                                categoryLogos = catLogos,
                                isLoading = refreshJob?.isActive == true,
                                error = if (uiChannels.isNotEmpty()) null else it.error,
                                errorType = if (uiChannels.isNotEmpty()) ErrorType.NONE else it.errorType,
                                selectedCategory = category
                            )
                        }
                    }
                    is Result.Error -> {
                        val (msg, type) = classifyError(result.exception)
                        _uiState.update {
                            it.copy(
                                isLoading = refreshJob?.isActive == true,
                                error = msg,
                                errorType = type
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
        recentlyWatchedJob?.cancel()
        popularCategoriesJob?.cancel()
        // Recently watched — auto-updates when user watches a new channel
        recentlyWatchedJob = viewModelScope.launch {
            try {
                playbackPositionDao.observeRecentlyWatchedIds(RECENTLY_WATCHED_LIMIT)
                    .flatMapLatest { recentIds ->
                        if (recentIds.isEmpty()) {
                            val empty = emptyList<ChannelUiModel>()
                            flowOf(empty to empty)
                        } else {
                            channelHealthDao.getAllHealth()
                                .debounce(HEALTH_SCAN_DEBOUNCE_MS)
                                .onStart { emit(emptyList()) }
                                .map { health ->
                                    val recentEntities = channelDao.getChannelsByIds(recentIds)
                                    val favIds = favoriteDao.getFavoriteChannelIds().toSet()
                                    val recentUi = channelUiMapper.toUiModelsWithHealth(
                                        recentEntities.map { channelMapper.toDomain(it, it.id in favIds) },
                                        health
                                    ).map { enrichWithEpgIfReady(it) }

                                    // Preserve the order from recentIds
                                    val idOrder = recentIds.withIndex().associate { (i, id) -> id to i }
                                    val sortedRecent = recentUi.sortedBy { idOrder[it.id] ?: Int.MAX_VALUE }
                                    sortedRecent to sortedRecent.take(FEATURED_CHANNELS_LIMIT)
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
        popularCategoriesJob = viewModelScope.launch {
            try {
                combine(
                    playbackPositionDao.observePopularCategoryIds(POPULAR_CATEGORIES_LIMIT),
                    favoriteCategoryDao.getAllFavoriteCategoryNames(),
                    channelDao.getAllChannels(),
                    channelHealthDao.getAllHealth()
                        .debounce(HEALTH_SCAN_DEBOUNCE_MS)
                        .onStart { emit(emptyList()) }
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
                        PopularCategoryUiModel(
                            name = catName,
                            channelCount = catChannels.size,
                            imageUrl = catChannels.firstOrNull { it.thumbnailPath != null }?.thumbnailPath
                                ?: catChannels.firstOrNull { it.logoUrl != null }?.logoUrl,
                            isFavorite = catName in favNames
                        )
                    }
                }
                    .debounce(CATEGORY_UPDATE_DEBOUNCE_MS)
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
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.channels.isEmpty()) }

            val result = refreshChannelsUseCase(Unit)

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isInitialLoadComplete = true) }
                }
                is Result.Error -> {
                    val hasCache = _uiState.value.channels.isNotEmpty()
                    if (hasCache) {
                        // Silently swallow refresh errors when cached data exists
                        _uiState.update { it.copy(isLoading = false, isInitialLoadComplete = true) }
                    } else {
                        val (msg, type) = classifyError(result.exception)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialLoadComplete = true,
                                error = msg,
                                errorType = type
                            )
                        }
                    }
                }
            }
        }
    }

    fun onResume() {
        if (!hasResumedBefore) {
            hasResumedBefore = true
            return // Skip first resume — init{} already handles it
        }
        refresh()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, errorType = ErrorType.NONE) }
    }

    private fun enrichWithEpgIfReady(channel: ChannelUiModel): ChannelUiModel {
        val tvgId = channel.tvgId ?: return channel
        val (now, _) = epgRepository.getNowNextIfCached(tvgId) ?: return channel
        return if (now != null) channel.copy(nowProgramTitle = now.title) else channel
    }

    private fun classifyError(exception: Exception): Pair<String, ErrorType> {
        return when (exception) {
            is UnauthorizedException, is ForbiddenException ->
                "Device not paired — please pair your TV" to ErrorType.AUTH_REQUIRED
            is NetworkException, is java.net.ConnectException,
            is java.net.UnknownHostException, is java.net.SocketTimeoutException ->
                "Cannot connect to server" to ErrorType.NETWORK_ERROR
            is ServerException ->
                "Server error — please try again later" to ErrorType.SERVER_ERROR
            is ServiceUnavailableException ->
                "Server is offline — please try again later" to ErrorType.SERVER_ERROR
            else ->
                (exception.message ?: "Something went wrong") to ErrorType.UNKNOWN
        }
    }
}
