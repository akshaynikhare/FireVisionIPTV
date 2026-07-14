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

/**
 * One channel row: the channel plus its programs across the visible window.
 *
 * [isHydrated] distinguishes a row whose programs have been loaded from one that is
 * still a placeholder — rows load lazily as they scroll into view so the guide scales
 * to very large channel lists without building every program cell up front.
 */
data class GuideRowUiModel(
    val channelId: String,
    val channelName: String,
    val channelNumber: Int,
    val logoUrl: String?,
    val category: String,
    val programs: List<GuideProgramUiModel>,
    val isHydrated: Boolean = false
)

/** Identifies the program currently focused, used to drive the detail panel. */
data class GuideFocusedProgram(
    val rowIndex: Int,
    val program: GuideProgramUiModel,
    val channelName: String
)

/** Which rows the guide is showing. Filtering is in-memory over the full channel set. */
sealed interface GuideFilter {
    data object All : GuideFilter
    data object Favorites : GuideFilter
    data class Category(val name: String) : GuideFilter
}

data class GuideUiState(
    val rows: List<GuideRowUiModel> = emptyList(),
    /** Distinct channel categories, in first-seen order, for the filter bar. */
    val categories: List<String> = emptyList(),
    val selectedFilter: GuideFilter = GuideFilter.All,
    /** True when the user has at least one favorite channel (drives the ★ chip). */
    val hasFavorites: Boolean = false,
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
