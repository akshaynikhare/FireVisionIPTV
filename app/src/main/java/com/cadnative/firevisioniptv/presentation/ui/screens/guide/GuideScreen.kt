package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.presentation.model.ErrorType
import com.cadnative.firevisioniptv.presentation.ui.components.EmptyState
import com.cadnative.firevisioniptv.presentation.ui.components.ErrorState
import com.cadnative.firevisioniptv.presentation.ui.components.LoadingIndicator
import com.cadnative.firevisioniptv.presentation.viewmodel.GuideViewModel

/**
 * EPG Guide — a channels × time-slot program grid.
 *
 * Thin stateful root: owns the ViewModel and top-level state branching (loading /
 * error / empty / content). The grid, timeline and cells live in same-package
 * `internal` files. Center on a program tunes to that channel via [onChannelClick];
 * Back returns to the previous screen.
 */
@Composable
fun GuideScreen(
    onNavigateBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCatchup: (channelId: String, startMillis: Long, durationMinutes: Int) -> Unit =
        { id, _, _ -> onChannelClick(id) },
    onPairDevice: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Catch-up is only deterministic for Xtream sources (timeshift URL).
    val catchupEnabled = remember {
        AppPreferences.getPlaylistSourceType(context) == AppPreferences.SOURCE_XTREAM
    }

    BackHandler(enabled = true) { onNavigateBack() }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.isEmpty ->
                LoadingIndicator(message = "Loading guide…")

            uiState.error != null && uiState.isEmpty ->
                ErrorState(
                    message = uiState.error!!,
                    errorType = uiState.errorType,
                    onRetry = viewModel::retry,
                    onPairDevice = if (uiState.errorType == ErrorType.AUTH_REQUIRED) onPairDevice else null
                )

            uiState.isEmpty ->
                EmptyState(
                    message = "No channels available for the guide",
                    onRetry = viewModel::retry
                )

            else ->
                GuideContent(
                    state = uiState,
                    onProgramSelected = { channelId, program ->
                        val ended = program.endTime.isBefore(java.time.Instant.now())
                        if (catchupEnabled && ended) {
                            val durationMinutes = java.time.Duration
                                .between(program.startTime, program.endTime)
                                .toMinutes().toInt().coerceAtLeast(1)
                            onCatchup(channelId, program.startTime.toEpochMilli(), durationMinutes)
                        } else {
                            onChannelClick(channelId)
                        }
                    },
                    onChannelSelected = onChannelClick,
                    modifier = Modifier.fillMaxSize()
                )
        }
    }
}
