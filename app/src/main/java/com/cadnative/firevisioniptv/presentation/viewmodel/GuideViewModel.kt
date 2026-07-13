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
import com.cadnative.firevisioniptv.domain.usecase.GetGuideProgramsUseCase
import com.cadnative.firevisioniptv.presentation.mapper.GuideUiMapper
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.model.GuideUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Hours of timeline shown on the horizontal axis. EPG is now persisted client-side
 * (Room cache), so we can show well beyond the old 4h window; the grid scrolls
 * horizontally at a fixed dp-per-minute so wider windows just add scroll length.
 */
private const val GUIDE_WINDOW_HOURS = 12L
private const val SLOT_MINUTES = 30L

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getGuideProgramsUseCase: GetGuideProgramsUseCase,
    private val guideUiMapper: GuideUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

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

        val now = Instant.now()
        // Round the axis start down to the nearest 30-min slot for tidy gridlines.
        val slot = Duration.ofMinutes(SLOT_MINUTES).seconds
        val windowStart = Instant.ofEpochSecond(now.epochSecond / slot * slot)
        val windowEnd = windowStart.plus(Duration.ofHours(GUIDE_WINDOW_HOURS))

        val tvgIds = channels.mapNotNull { it.tvgId }.distinct()
        val programsByTvgId = if (tvgIds.isEmpty()) {
            emptyMap()
        } else {
            getGuideProgramsUseCase(
                GetGuideProgramsUseCase.Params(tvgIds, windowStart, windowEnd)
            )
        }

        val rows = guideUiMapper.toRows(channels, programsByTvgId, windowStart, windowEnd, now)
        val hasTimeline = rows.any { it.programs.isNotEmpty() }

        _uiState.update {
            it.copy(
                rows = rows,
                windowStart = windowStart,
                windowEnd = windowEnd,
                isLoading = false,
                error = null,
                errorType = ErrorType.NONE,
                timelineUnavailable = !hasTimeline
            )
        }
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
