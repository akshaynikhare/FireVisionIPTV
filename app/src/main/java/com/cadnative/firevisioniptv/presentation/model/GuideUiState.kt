package com.cadnative.firevisioniptv.presentation.model

import java.time.Instant

/**
 * A single program cell in the Guide grid, pre-resolved to the values the UI needs.
 * Widths are derived from [startTime]/[endTime] against the visible window.
 */
data class GuideProgramUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: Instant,
    val endTime: Instant,
    val isLive: Boolean
)

/** One channel row: the channel plus its programs across the visible window. */
data class GuideRowUiModel(
    val channelId: String,
    val channelName: String,
    val channelNumber: Int,
    val logoUrl: String?,
    val category: String,
    val programs: List<GuideProgramUiModel>
)

/** Identifies the program currently focused, used to drive the detail panel. */
data class GuideFocusedProgram(
    val rowIndex: Int,
    val program: GuideProgramUiModel,
    val channelName: String
)

data class GuideUiState(
    val rows: List<GuideRowUiModel> = emptyList(),
    /** Inclusive start of the visible time axis. */
    val windowStart: Instant = Instant.now(),
    /** Exclusive end of the visible time axis. */
    val windowEnd: Instant = Instant.now(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val errorType: ErrorType = ErrorType.NONE,
    /** True when channels loaded but no program timeline was available (now/next-only server). */
    val timelineUnavailable: Boolean = false
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}
