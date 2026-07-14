package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.remote.ForbiddenException
import com.cadnative.firevisioniptv.data.source.remote.NetworkException
import com.cadnative.firevisioniptv.data.source.remote.ServerException
import com.cadnative.firevisioniptv.data.source.remote.ServiceUnavailableException
import com.cadnative.firevisioniptv.data.source.remote.UnauthorizedException
import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetFavoriteChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetGuideProgramsUseCase
import com.cadnative.firevisioniptv.presentation.mapper.GuideUiMapper
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.model.GuideFilter
import com.cadnative.firevisioniptv.presentation.model.GuideRowUiModel
import com.cadnative.firevisioniptv.presentation.model.GuideUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Hours of timeline shown on the horizontal axis. EPG is persisted client-side
 * (Room cache), so we can show well beyond the old 4h window; the grid scrolls
 * horizontally at a fixed dp-per-minute so wider windows just add scroll length.
 */
private const val GUIDE_WINDOW_HOURS = 12L
private const val SLOT_MINUTES = 30L

/** Rows above/below the viewport to pre-hydrate so scrolling rarely reveals a placeholder. */
private const val HYDRATION_BUFFER = 20
/** Rows on either side of the viewport whose programs are retained; the rest are evicted. */
private const val KEEP_MARGIN = 120
/** Rows hydrated eagerly on first load / filter change so the guide never opens all-skeleton. */
private const val INITIAL_ROWS = 30

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val getGuideProgramsUseCase: GetGuideProgramsUseCase,
    private val guideUiMapper: GuideUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    // ── Full (unfiltered) working set, kept off the UI state so filtering/hydration is in-memory ──
    private var skeletonRows: List<GuideRowUiModel> = emptyList()
    private var tvgIdByChannelId: Map<String, String?> = emptyMap()
    private var favoriteIds: Set<String> = emptySet()
    private var selectedFilter: GuideFilter = GuideFilter.All
    private var windowStart: Instant = Instant.now()
    private var windowEnd: Instant = Instant.now()
    private var buildNow: Instant = Instant.now()

    /** channelId → hydrated program cells; the guide's live memory cap. */
    private val programCache = HashMap<String, List<com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel>>()
    private val cacheMutex = Mutex()

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorType = ErrorType.NONE) }

            val result = getChannelsUseCase(Unit).first()
            when (result) {
                is Result.Success -> buildGuide(result.data)
                is Result.Error -> {
                    val (msg, type) = classifyError(result.exception)
                    _uiState.update { it.copy(isLoading = false, error = msg, errorType = type) }
                }
            }
        }
    }

    private suspend fun buildGuide(channels: List<Channel>) {
        if (channels.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, rows = emptyList()) }
            return
        }

        // Round the axis start down to the nearest 30-min slot for tidy gridlines.
        buildNow = Instant.now()
        val slot = Duration.ofMinutes(SLOT_MINUTES).seconds
        windowStart = Instant.ofEpochSecond(buildNow.epochSecond / slot * slot)
        windowEnd = windowStart.plus(Duration.ofHours(GUIDE_WINDOW_HOURS))

        skeletonRows = guideUiMapper.toSkeletonRows(channels)
        tvgIdByChannelId = channels.associate { it.id to it.tvgId }
        favoriteIds = loadFavoriteIds()
        selectedFilter = GuideFilter.All
        programCache.clear()

        val categories = channels.map { it.category }.filter { it.isNotBlank() }.distinct()
        val hasTvgIds = channels.any { !it.tvgId.isNullOrBlank() }

        _uiState.update {
            it.copy(
                categories = categories,
                selectedFilter = GuideFilter.All,
                hasFavorites = favoriteIds.isNotEmpty(),
                windowStart = windowStart,
                windowEnd = windowEnd,
                isLoading = false,
                error = null,
                errorType = ErrorType.NONE,
                timelineUnavailable = !hasTvgIds
            )
        }

        // Eagerly hydrate the first screenful so the guide never opens all-placeholder.
        hydrateRange(0, INITIAL_ROWS)

        // Keep the ★ Favorites chip and filter in sync as favorites change elsewhere.
        observeFavorites()
    }

    private suspend fun loadFavoriteIds(): Set<String> =
        when (val res = getFavoriteChannelsUseCase(Unit).first()) {
            is Result.Success -> res.data.map { it.id }.toSet()
            is Result.Error -> emptySet()
        }

    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            getFavoriteChannelsUseCase(Unit).collect { res ->
                val ids = (res as? Result.Success)?.data?.map { it.id }?.toSet() ?: emptySet()
                if (ids == favoriteIds) return@collect
                favoriteIds = ids
                _uiState.update { it.copy(hasFavorites = ids.isNotEmpty()) }
                // Re-derive the rows only when the change is actually on screen.
                if (selectedFilter is GuideFilter.Favorites) hydrateRange(0, INITIAL_ROWS)
            }
        }
    }

    /** Called by the grid as the viewport moves; hydrates the visible window and evicts the rest. */
    fun onVisibleRangeChanged(firstVisible: Int, lastVisible: Int) {
        viewModelScope.launch {
            hydrateRange(firstVisible - HYDRATION_BUFFER, lastVisible + HYDRATION_BUFFER)
        }
    }

    fun selectFilter(filter: GuideFilter) {
        if (filter == selectedFilter) return
        selectedFilter = filter
        _uiState.update { it.copy(selectedFilter = filter) }
        // Recompute the displayed list and pre-hydrate its first screenful (the grid resets to top).
        viewModelScope.launch { hydrateRange(0, INITIAL_ROWS) }
    }

    /**
     * Hydrate programs for the displayed rows in `[from, to]`, publish, then drop cached
     * programs for rows more than [KEEP_MARGIN] outside the range so memory stays bounded
     * regardless of channel count.
     */
    private suspend fun hydrateRange(from: Int, to: Int) {
        val displayed = displayedSkeleton()
        // Publish (don't early-return) so switching to an empty filter clears the grid
        // instead of leaving the previous filter's rows on screen.
        if (displayed.isEmpty()) {
            publishRows(displayed)
            return
        }

        val lo = from.coerceIn(0, displayed.lastIndex)
        val hi = to.coerceIn(0, displayed.lastIndex)
        val windowIds = (lo..hi).map { displayed[it].channelId }
        val missing = windowIds.filter { it !in programCache }

        cacheMutex.withLock {
            if (missing.isNotEmpty()) {
                val tvgIds = missing.mapNotNull { tvgIdByChannelId[it] }.filter { it.isNotBlank() }.distinct()
                val programsByTvgId = if (tvgIds.isEmpty()) {
                    emptyMap()
                } else {
                    getGuideProgramsUseCase(
                        GetGuideProgramsUseCase.Params(tvgIds, windowStart, windowEnd)
                    )
                }
                missing.forEach { channelId ->
                    val raw = tvgIdByChannelId[channelId]?.let { programsByTvgId[it] }.orEmpty()
                    programCache[channelId] = guideUiMapper.toPrograms(raw, buildNow)
                }
            }
            // Evict rows well outside the retained band around the viewport.
            val keepFrom = (lo - KEEP_MARGIN).coerceAtLeast(0)
            val keepTo = (hi + KEEP_MARGIN).coerceAtMost(displayed.lastIndex)
            val keepIds = (keepFrom..keepTo).mapTo(HashSet()) { displayed[it].channelId }
            programCache.keys.retainAll(keepIds)
        }

        publishRows(displayed)
    }

    /** The filtered skeleton (no programs attached) in stable channel order. */
    private fun displayedSkeleton(): List<GuideRowUiModel> = when (val f = selectedFilter) {
        is GuideFilter.All -> skeletonRows
        is GuideFilter.Favorites -> skeletonRows.filter { it.channelId in favoriteIds }
        is GuideFilter.Category -> skeletonRows.filter { it.category.equals(f.name, ignoreCase = true) }
    }

    /** Attach cached programs to the displayed skeleton and push to the UI. */
    private fun publishRows(displayed: List<GuideRowUiModel>) {
        val rows = displayed.map { row ->
            val cached = programCache[row.channelId]
            if (cached != null) row.copy(programs = cached, isHydrated = true) else row
        }
        _uiState.update { it.copy(rows = rows) }
    }

    fun retry() = load()

    private fun classifyError(exception: Exception): Pair<String, ErrorType> = when (exception) {
        is UnauthorizedException, is ForbiddenException ->
            "Device not paired — please pair your device" to ErrorType.AUTH_REQUIRED
        is NetworkException, is java.net.ConnectException,
        is java.net.UnknownHostException, is java.net.SocketTimeoutException ->
            "Cannot connect to server — check server URL in Settings" to ErrorType.NETWORK_ERROR
        is ServerException ->
            "Server error — please try again later" to ErrorType.SERVER_ERROR
        is ServiceUnavailableException ->
            "Server is offline — please try again later" to ErrorType.SERVER_ERROR
        else ->
            (exception.message ?: "Something went wrong") to ErrorType.UNKNOWN
    }
}
