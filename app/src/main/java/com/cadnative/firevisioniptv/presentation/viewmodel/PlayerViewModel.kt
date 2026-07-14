package com.cadnative.firevisioniptv.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import com.cadnative.firevisioniptv.domain.repository.UserPreferencesRepository
import com.cadnative.firevisioniptv.domain.service.AnalyticsHelper
import com.cadnative.firevisioniptv.domain.service.ChannelThumbnailExtractor
import com.cadnative.firevisioniptv.domain.usecase.GetChannelByIdUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsByCategoryUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetChannelsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetGuideProgramsUseCase
import com.cadnative.firevisioniptv.domain.usecase.GetPlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReportStreamPlayUseCase
import com.cadnative.firevisioniptv.domain.usecase.ReportStreamStatusUseCase
import com.cadnative.firevisioniptv.domain.usecase.SavePlaybackPositionUseCase
import com.cadnative.firevisioniptv.domain.usecase.ToggleFavoriteUseCase
import com.cadnative.firevisioniptv.presentation.mapper.ChannelUiMapper
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.presentation.ui.player.PlayerFactory
import com.cadnative.firevisioniptv.presentation.ui.player.StreamErrorContext
import com.cadnative.firevisioniptv.presentation.ui.player.StreamErrorMessageResolver
import com.cadnative.firevisioniptv.presentation.ui.animation.AUTO_HIDE_DELAY_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val POSITION_SAVE_INTERVAL_MS = 5_000L
private const val DEAD_STREAM_COUNTDOWN_SECONDS = 5
private const val COUNTDOWN_TICK_MS = 1_000L
private const val FINAL_SAVE_TIMEOUT_MS = 3_000L
private const val PLAY_REPORT_THRESHOLD_MS = 10_000L
private const val SLEEP_TIMER_FINE_THRESHOLD_SECONDS = 60
private const val SLEEP_TIMER_COARSE_TICK_SECONDS = 30
private const val STILL_WATCHING_WINDOW_MS = 60_000L
private const val INACTIVITY_TIMEOUT_MS = 4 * 60 * 60 * 1_000L
private const val INACTIVITY_CHECK_INTERVAL_MS = 60_000L
private const val MAX_RECENT_CHANNELS = 3
private const val BOUNDARY_GRACE_MS = 2_000L      // let the clock actually pass endTime
private const val BOUNDARY_MIN_DELAY_MS = 5_000L  // floor so a stale/past endTime can't spin
private const val EPG_TICK_MS = 60_000L

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getChannelByIdUseCase: GetChannelByIdUseCase,
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase,
    private val getPlaybackPositionUseCase: GetPlaybackPositionUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val reportStreamStatusUseCase: ReportStreamStatusUseCase,
    private val reportStreamPlayUseCase: ReportStreamPlayUseCase,
    private val channelUiMapper: ChannelUiMapper,
    private val channelHealthDao: ChannelHealthDao,
    private val thumbnailExtractor: ChannelThumbnailExtractor,
    private val epgRepository: EpgRepository,
    private val getGuideProgramsUseCase: GetGuideProgramsUseCase,
    private val analyticsHelper: AnalyticsHelper,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val playerFactory: PlayerFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Builds an IPTV-tuned ExoPlayer (buffers, timeouts, decoder fallback). */
    fun createPlayer(): ExoPlayer = playerFactory.create()

    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.getPlayerKeyUpDownAction(),
                userPreferencesRepository.getPlayerKeyLeftRightAction(),
                userPreferencesRepository.getPlayerLongOkAction()
            ) { upDown, leftRight, longOk ->
                _uiState.update {
                    it.copy(
                        keyUpDownAction = upDown,
                        keyLeftRightAction = leftRight,
                        longOkAction = longOk
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            userPreferencesRepository.getSleepTimerDefaultMinutes().collect {
                sleepTimerDefaultMinutes = it
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.getAlwaysShowProgramBar().collect { enabled ->
                _uiState.update { it.copy(alwaysShowProgramBar = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.getInfoBarTimeoutSeconds().collect { seconds ->
                _uiState.update { it.copy(infoBarTimeoutSeconds = seconds) }
            }
        }
    }

    private var savePositionJob: Job? = null
    private var loadJob: Job? = null
    private var playbackPositionJob: Job? = null
    private var channelListJob: Job? = null
    private var preloadJob: Job? = null
    private var autoHideJob: Job? = null
    private var countdownJob: Job? = null
    private var playReportJob: Job? = null
    private var epgJob: Job? = null
    private var boundaryJob: Job? = null
    private var scheduleJob: Job? = null
    private var epgTickJob: Job? = null
    private var epgEnrichKickJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var stillWatchingJob: Job? = null
    private var inactivityJob: Job? = null
    private var playReportedForChannel: String? = null
    private var channelViewStartTime: Long = 0L
    private var sleepTimerDefaultMinutes: Int = 0
    private var sleepTimerDefaultApplied = false
    private var lastInteractionTime: Long = System.currentTimeMillis()

    private fun logWatchDuration() {
        val state = _uiState.value
        val channel = state.channel ?: return
        if (channelViewStartTime == 0L) return
        val durationSeconds = (System.currentTimeMillis() - channelViewStartTime) / 1000
        if (durationSeconds < 1) return
        analyticsHelper.logEvent(
            "channel_watch_duration",
            "channel_id" to channel.id,
            "channel_name" to channel.name.take(100),
            "category" to channel.category.take(100),
            "duration_seconds" to durationSeconds
        )
        channelViewStartTime = 0L
    }

    fun loadChannel(channelId: String) {
        logWatchDuration()
        loadJob?.cancel()
        playReportJob?.cancel()
        playReportedForChannel = null
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getChannelByIdUseCase(channelId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val channel = result.data
                        val uiModel = channelUiMapper.toUiModel(channel)
                        _uiState.update {
                            it.copy(
                                channel = uiModel,
                                isLoading = false,
                                error = null,
                                nowPlaying = null,
                                nextProgram = null
                            )
                        }
                        channelViewStartTime = System.currentTimeMillis()
                        analyticsHelper.logEvent(
                            "channel_view",
                            "channel_id" to channel.id,
                            "channel_name" to uiModel.name.take(100),
                            "category" to uiModel.category.take(100)
                        )
                        loadPlaybackPosition(channelId)
                        // Preload channel list for next/prev navigation
                        preloadChannelList()
                        // Fetch EPG now/next non-blocking
                        boundaryJob?.cancel()
                        fetchEpg(channel.tvgId)
                        loadSchedule(channel.tvgId)
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

    private fun fetchEpg(tvgId: String?) {
        if (tvgId.isNullOrBlank()) return
        epgJob?.cancel()
        epgJob = viewModelScope.launch {
            try {
                val result = epgRepository.getNowNext(tvgId)
                val now = result.first
                val next = result.second
                _uiState.update { it.copy(nowPlaying = now, nextProgram = next) }
                if (now != null) scheduleProgramBoundary(tvgId, now)
            } catch (_: Exception) {
                // EPG failure is non-fatal; overlay shows channel info only
            }
        }
    }

    /**
     * Wakes at the current program's end time, refetches now/next, and bumps
     * [PlayerUiState.programChangedToken] on a real transition so the UI can
     * re-reveal the info banner. A stale guide (endTime already past, same
     * program returned) gets one retry, then the watcher stops until the next
     * zap restarts it via [fetchEpg].
     */
    private fun scheduleProgramBoundary(tvgId: String, firstCurrent: EpgProgram) {
        boundaryJob?.cancel()
        val channelId = _uiState.value.channel?.id ?: return
        boundaryJob = viewModelScope.launch {
            var current = firstCurrent
            var retried = false
            while (true) {
                delay(
                    (current.endTime.toEpochMilli() - System.currentTimeMillis() + BOUNDARY_GRACE_MS)
                        .coerceAtLeast(BOUNDARY_MIN_DELAY_MS)
                )
                if (_uiState.value.channel?.id != channelId) return@launch
                val (now, next) = try {
                    epgRepository.getNowNext(tvgId)
                } catch (_: Exception) {
                    return@launch
                }
                _uiState.update { it.copy(nowPlaying = now, nextProgram = next) }
                when {
                    now == null -> return@launch // guide exhausted
                    now.startTime != current.startTime -> {
                        _uiState.update { it.copy(programChangedToken = it.programChangedToken + 1) }
                        current = now
                        retried = false
                    }
                    retried -> return@launch // stale guide — give up until next zap
                    else -> {
                        current = now
                        retried = true
                    }
                }
            }
        }
    }

    // ── Overlay EPG enrichment (now/next + progress on channel lists) ──

    private fun epgKey(tvgId: String?): String? =
        tvgId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    private fun enrichWithEpgIfReady(channel: ChannelUiModel): ChannelUiModel {
        val tvgId = channel.tvgId ?: return channel
        // Null = cache not hydrated (keep whatever we have); a loaded pair always
        // overwrites so stale programs clear once the guide moves on.
        val (now, next) = epgRepository.getNowNextIfCached(tvgId) ?: return channel
        return channel.copy(
            nowProgramTitle = now?.title,
            nextProgramTitle = next?.title,
            nowProgramStartMs = now?.startTime?.toEpochMilli(),
            nowProgramEndMs = now?.endTime?.toEpochMilli()
        )
    }

    private fun buildOverlayEpg(channels: List<ChannelUiModel>): Map<String, Pair<EpgProgram?, EpgProgram?>> =
        channels.mapNotNull { epgKey(it.tvgId) }.distinct()
            .mapNotNull { key -> epgRepository.getNowNextIfCached(key)?.let { key to it } }
            .toMap()

    private fun reEnrichOverlay() {
        _uiState.update { state ->
            val enriched = state.overlayChannels.map(::enrichWithEpgIfReady)
            state.copy(overlayChannels = enriched, overlayEpg = buildOverlayEpg(enriched))
        }
        scheduleOverlayEpgRefresh()
    }

    /** One-shot: once the EPG cache hydrates, re-enrich whatever list is showing. */
    private fun kickEpgEnrichment() {
        if (epgEnrichKickJob?.isActive == true) return
        epgEnrichKickJob = viewModelScope.launch {
            try {
                epgRepository.ensureLoaded()
            } catch (_: Exception) {
                return@launch
            }
            reEnrichOverlay()
        }
    }

    /**
     * One-shot refresh armed at the earliest listed program's end time (60s
     * floor so a stale guide can't spin). Re-arms itself through
     * [reEnrichOverlay] only while there is program data to expire.
     */
    private fun scheduleOverlayEpgRefresh() {
        epgTickJob?.cancel()
        val earliestEnd = _uiState.value.overlayChannels
            .mapNotNull { it.nowProgramEndMs }
            .minOrNull() ?: return
        epgTickJob = viewModelScope.launch {
            delay(
                (earliestEnd - System.currentTimeMillis() + BOUNDARY_GRACE_MS)
                    .coerceAtLeast(EPG_TICK_MS)
            )
            reEnrichOverlay()
        }
    }

    /** Load today's schedule for the current channel (mobile Schedule tab). */
    private fun loadSchedule(tvgId: String?) {
        scheduleJob?.cancel()
        if (tvgId.isNullOrBlank()) {
            _uiState.update { it.copy(schedulePrograms = emptyList(), scheduleLoading = false) }
            return
        }
        scheduleJob = viewModelScope.launch {
            _uiState.update { it.copy(scheduleLoading = true) }
            val zone = java.time.ZoneId.systemDefault()
            val startOfDay = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val endOfDay = startOfDay.plus(java.time.Duration.ofDays(1))
            val programs = try {
                getGuideProgramsUseCase(GetGuideProgramsUseCase.Params(listOf(tvgId), startOfDay, endOfDay))
                    .values.firstOrNull().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(schedulePrograms = programs, scheduleLoading = false) }
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, position: Long, duration: Long) {
        _uiState.update {
            it.copy(isPlaying = isPlaying, position = position, duration = duration)
        }
        if (isPlaying) {
            startPeriodicPositionSaving()
            startPlayReportTimer()
            if (!sleepTimerDefaultApplied && sleepTimerDefaultMinutes > 0) {
                setSleepTimer(sleepTimerDefaultMinutes)
            }
            startInactivityWatch()
        } else {
            stopPeriodicPositionSaving()
            saveCurrentPosition()
            playReportJob?.cancel()
            inactivityJob?.cancel()
        }
    }

    // ── Sleep Timer & Auto-Off ──────────────────────────────────────

    /**
     * Start (or cancel with null/0) the sleep timer. Counts down with cheap
     * 30s ticks until the final minute, then 1s ticks so the UI countdown
     * chip stays accurate.
     */
    fun setSleepTimer(minutes: Int?) {
        sleepTimerDefaultApplied = true
        sleepTimerJob?.cancel()
        stillWatchingJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update {
                it.copy(
                    sleepTimerMinutes = null,
                    sleepTimerRemainingSeconds = null,
                    sleepTimerExpired = false,
                    sleepTimerNavigateBack = false
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                sleepTimerMinutes = minutes,
                sleepTimerRemainingSeconds = minutes * 60,
                sleepTimerExpired = false,
                sleepTimerNavigateBack = false
            )
        }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                val tickSeconds = if (remaining > SLEEP_TIMER_FINE_THRESHOLD_SECONDS) {
                    (remaining - SLEEP_TIMER_FINE_THRESHOLD_SECONDS)
                        .coerceAtMost(SLEEP_TIMER_COARSE_TICK_SECONDS)
                } else {
                    1
                }
                delay(tickSeconds * 1_000L)
                remaining -= tickSeconds
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            onSleepTimerExpired()
        }
    }

    /**
     * Expiry: UI pauses playback and shows "Still watching?". If not
     * cancelled within the window, request navigation back.
     */
    private fun onSleepTimerExpired() {
        _uiState.update { it.copy(sleepTimerExpired = true, sleepTimerRemainingSeconds = 0) }
        stillWatchingJob?.cancel()
        stillWatchingJob = viewModelScope.launch {
            delay(STILL_WATCHING_WINDOW_MS)
            _uiState.update { it.copy(sleepTimerNavigateBack = true) }
        }
    }

    /** Any key press during the "Still watching?" window keeps the session alive. */
    fun cancelSleepTimerExpiry() {
        sleepTimerJob?.cancel()
        stillWatchingJob?.cancel()
        lastInteractionTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                sleepTimerMinutes = null,
                sleepTimerRemainingSeconds = null,
                sleepTimerExpired = false,
                sleepTimerNavigateBack = false
            )
        }
    }

    fun onSleepTimerNavigatedBack() {
        _uiState.update { it.copy(sleepTimerNavigateBack = false) }
    }

    /** Called from the player key path to track activity for the auto-off guard. */
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Auto-off guard: with no sleep timer set, 4 hours without a key press
     * during playback triggers the same "Still watching?" flow.
     */
    private fun startInactivityWatch() {
        if (inactivityJob?.isActive == true) return
        inactivityJob = viewModelScope.launch {
            while (true) {
                delay(INACTIVITY_CHECK_INTERVAL_MS)
                val state = _uiState.value
                if (state.sleepTimerMinutes != null || state.sleepTimerExpired) continue
                if (System.currentTimeMillis() - lastInteractionTime >= INACTIVITY_TIMEOUT_MS) {
                    onSleepTimerExpired()
                    return@launch
                }
            }
        }
    }

    private fun startPlayReportTimer() {
        val channelId = _uiState.value.channel?.id ?: return
        if (playReportedForChannel == channelId) return
        playReportJob?.cancel()
        playReportJob = viewModelScope.launch {
            delay(PLAY_REPORT_THRESHOLD_MS)
            if (_uiState.value.isPlaying) {
                playReportedForChannel = channelId
                reportStreamPlayUseCase(
                    ReportStreamPlayUseCase.Params(
                        channelId = channelId,
                        proxyPlay = _uiState.value.isUsingProxy,
                        streamUrl = _uiState.value.activeStreamUrl
                    )
                )
            }
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

    private var currentSaveJob: Job? = null

    private fun saveCurrentPosition() {
        val state = _uiState.value
        val channelId = state.channel?.id ?: return
        currentSaveJob?.cancel()
        currentSaveJob = viewModelScope.launch {
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
        // Optimistically update UI immediately so indicator shows correct text
        _uiState.update { state ->
            state.channel?.let { channel ->
                state.copy(channel = channel.copy(isFavorite = !channel.isFavorite))
            } ?: state
        }
        viewModelScope.launch {
            val result = toggleFavoriteUseCase(channelId)
            if (result is Result.Error) {
                // Revert on failure
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
            // NonCancellable: this fires during onDispose — viewModelScope may be
            // closing, but the disk write + DB upsert must complete.
            withContext(NonCancellable) {
                thumbnailExtractor.saveThumbnailBitmap(channelId, bitmap)
            }
        }
    }

    // ── Channel Overlay ─────────────────────────────────────────────

    fun showOverlay() {
        _uiState.update { it.copy(showChannelOverlay = true) }
        // Always select the current channel's category so the user sees similar channels
        val currentCategory = _uiState.value.channel?.category
        loadChannelList(currentCategory)
        resetAutoHideTimer()
    }

    fun hideOverlay() {
        _uiState.update { it.copy(showChannelOverlay = false) }
        autoHideJob?.cancel()
    }

    /**
     * Preload all channels so next/prev works even before overlay is opened.
     * Uses a separate job so it doesn't cancel/get cancelled by overlay category loads.
     * Skips if overlay is open or a user-initiated category load is active.
     */
    private fun preloadChannelList() {
        if (_uiState.value.showChannelOverlay) return
        if (channelListJob?.isActive == true) return
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            val channelFlow = getChannelsUseCase(Unit)
            channelFlow.combine(channelHealthDao.getAllHealth()) { result, healthList ->
                result to healthList
            }.collect { (result, healthList) ->
                when (result) {
                    is Result.Success -> {
                        val uiChannels = channelUiMapper.toUiModelsWithHealth(result.data, healthList)
                            .map(::enrichWithEpgIfReady)
                        val categories = uiChannels.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                        _uiState.update {
                            it.copy(
                                overlayChannels = uiChannels,
                                overlayEpg = buildOverlayEpg(uiChannels),
                                overlayCategories = categories,
                                overlayIsLoadingChannels = false,
                                // Full list — drop recents no longer in the playlist
                                recentChannels = it.recentChannels.filter { r -> uiChannels.any { c -> c.id == r.id } }
                            )
                        }
                        kickEpgEnrichment()
                        scheduleOverlayEpgRefresh()
                    }
                    is Result.Error -> { /* silent — preload is best-effort */ }
                }
            }
        }
    }

    fun loadChannelList(category: String? = null) {
        preloadJob?.cancel() // user-initiated load takes priority over background preload
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
                            .map(::enrichWithEpgIfReady)
                        // Always load all categories when loading all channels
                        val categories = if (category == null || _uiState.value.overlayCategories.isEmpty()) {
                            uiChannels.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                        } else {
                            _uiState.value.overlayCategories
                        }
                        _uiState.update {
                            it.copy(
                                overlayChannels = uiChannels,
                                overlayEpg = buildOverlayEpg(uiChannels),
                                overlayCategories = categories,
                                overlayIsLoadingChannels = false
                            )
                        }
                        kickEpgEnrichment()
                        scheduleOverlayEpgRefresh()
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(overlayIsLoadingChannels = false) }
                    }
                }
            }
        }
    }

    // ── Next / Previous Channel (D-Pad & remote buttons) ───────────

    /** Zap order for the mobile portrait Channels tab — same list ▲▼/swipe zap walks. */
    fun zapChannels(): List<ChannelUiModel> = zapList()

    /** Same-category channels excluding offline ones (current channel always kept). */
    private fun zapList(): List<ChannelUiModel> {
        val currentChannel = _uiState.value.channel ?: return emptyList()
        return _uiState.value.overlayChannels
            .filter { it.category == currentChannel.category &&
                    (it.id == currentChannel.id || it.healthStatus != ChannelHealthStatus.OFFLINE) }
    }

    fun nextChannel() {
        val currentChannel = _uiState.value.channel ?: return
        val channels = zapList()
        if (channels.size <= 1) return // only current channel or empty — nowhere to go
        val currentIndex = channels.indexOfFirst { it.id == currentChannel.id }
        val nextIndex = if (currentIndex < 0 || currentIndex >= channels.size - 1) 0 else currentIndex + 1
        switchChannel(channels[nextIndex].id)
    }

    fun previousChannel() {
        val currentChannel = _uiState.value.channel ?: return
        val channels = zapList()
        if (channels.size <= 1) return
        val currentIndex = channels.indexOfFirst { it.id == currentChannel.id }
        val prevIndex = if (currentIndex <= 0) channels.size - 1 else currentIndex - 1
        switchChannel(channels[prevIndex].id)
    }

    fun recallLastChannel() {
        _uiState.value.lastChannel?.let { switchChannel(it.id) }
    }

    /** Jump to the 1-based channel number within the current category (remote number keys). */
    fun switchToChannelNumber(number: Int) {
        val channels = zapList()
        val target = channels.getOrNull(number - 1) ?: return
        switchChannel(target.id)
    }

    fun switchChannel(channelId: String) {
        if (channelId == _uiState.value.channel?.id) {
            hideOverlay()
            return
        }
        _uiState.value.channel?.let { current ->
            _uiState.update { state ->
                state.copy(
                    recentChannels = (listOf(current) + state.recentChannels.filter { it.id != current.id })
                        .filter { it.id != channelId } // never stack the target we're switching to
                        .take(MAX_RECENT_CHANNELS)
                )
            }
        }
        logWatchDuration()
        boundaryJob?.cancel()
        countdownJob?.cancel()
        playReportJob?.cancel()
        playReportedForChannel = null
        _uiState.update {
            it.copy(
                isRecovering = false,
                isStreamDead = false,
                isUsingProxy = false,
                activeStreamUrl = null,
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
                        val channel = result.data
                        _uiState.update {
                            it.copy(
                                channel = channelUiMapper.toUiModel(channel),
                                isSwitchingChannel = false,
                                showChannelOverlay = false,
                                position = 0L,
                                duration = 0L,
                                nowPlaying = null,
                                nextProgram = null
                            )
                        }
                        loadPlaybackPosition(channelId)
                        fetchEpg(channel.tvgId)
                        loadSchedule(channel.tvgId)
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

    fun onProxyFallback() {
        _uiState.update { it.copy(isUsingProxy = true) }
    }

    fun onAlternateFallback(streamUrl: String) {
        _uiState.update { it.copy(activeStreamUrl = streamUrl, isUsingProxy = false) }
    }

    fun onStreamDead(errorMessage: String) {
        val channelId = _uiState.value.channel?.id ?: return
        val channelName = _uiState.value.channel?.name ?: ""
        val category = _uiState.value.channel?.category ?: ""

        logWatchDuration()
        analyticsHelper.logEvent(
            "channel_error",
            "channel_id" to channelId,
            "channel_name" to channelName.take(100),
            "category" to category.take(100),
            "error_type" to errorMessage.take(100)
        )

        _uiState.update {
            it.copy(
                isRecovering = false,
                isPlaying = false,
                isStreamDead = true,
                deadStreamTitle = "Stream Unavailable",
                deadStreamExplanation = "",
                error = null
            )
        }

        viewModelScope.launch {
            val previousHealth = channelHealthDao.getHealthByChannelId(channelId).firstOrNull()

            channelHealthDao.upsertPreservingThumbnail(
                channelId = channelId,
                status = ChannelHealthStatus.OFFLINE.name,
                lastCheckedAt = System.currentTimeMillis(),
                responseTimeMs = null,
                errorMessage = errorMessage
            )
            reportStreamStatusUseCase(
                ReportStreamStatusUseCase.Params(
                    channelId = channelId,
                    status = ReportStreamStatusUseCase.Status.DEAD,
                    errorMessage = errorMessage
                )
            )

            val offlineCount = channelHealthDao.getOfflineCountByCategory(category)
            val scannedCount = channelHealthDao.getScannedCountByCategory(category)
            val resolved = StreamErrorMessageResolver.resolve(
                StreamErrorContext(
                    errorMessage = errorMessage,
                    lastCheckedAt = previousHealth?.lastCheckedAt,
                    previousStatus = previousHealth?.status,
                    categoryOfflineCount = offlineCount,
                    categoryScannedCount = scannedCount
                )
            )
            _uiState.update {
                it.copy(
                    deadStreamTitle = resolved.title,
                    deadStreamExplanation = resolved.explanation
                )
            }
        }
        startDeadStreamCountdown()
    }

    fun onStreamUnresponsive() {
        val channelId = _uiState.value.channel?.id ?: return
        viewModelScope.launch {
            channelHealthDao.upsertPreservingThumbnail(
                channelId = channelId,
                status = ChannelHealthStatus.UNRESPONSIVE.name,
                lastCheckedAt = System.currentTimeMillis(),
                responseTimeMs = null,
                errorMessage = "Stream unresponsive (buffering timeout)"
            )
            reportStreamStatusUseCase(
                ReportStreamStatusUseCase.Params(
                    channelId = channelId,
                    status = ReportStreamStatusUseCase.Status.UNRESPONSIVE
                )
            )
        }
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
        logWatchDuration()
        stopPeriodicPositionSaving()
        playbackPositionJob?.cancel()
        channelListJob?.cancel()
        autoHideJob?.cancel()
        countdownJob?.cancel()
        playReportJob?.cancel()
        epgJob?.cancel()
        boundaryJob?.cancel()
        scheduleJob?.cancel()
        epgTickJob?.cancel()
        epgEnrichKickJob?.cancel()
        sleepTimerJob?.cancel()
        stillWatchingJob?.cancel()
        inactivityJob?.cancel()
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
