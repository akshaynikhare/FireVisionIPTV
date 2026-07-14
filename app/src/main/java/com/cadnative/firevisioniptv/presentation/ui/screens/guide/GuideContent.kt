package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.cadnative.firevisioniptv.presentation.model.GuideFocusedProgram
import com.cadnative.firevisioniptv.presentation.model.GuideUiState
import com.cadnative.firevisioniptv.presentation.ui.components.ScreenScaffold
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Guide layout on the shared [ScreenScaffold]: "Guide" title band with the
 * focused-program details (or the timeline-unavailable notice) right-aligned in
 * the trailing slot, then filter chips, then the channels × time grid. Keeping
 * the details inside the fixed header band costs the description line but gives
 * the reclaimed panel height back to the grid, and the grid never jumps as
 * focus moves.
 */
@Composable
internal fun GuideContent(
    state: GuideUiState,
    onProgramSelected: (channelId: String, program: com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel) -> Unit,
    onChannelSelected: (channelId: String) -> Unit,
    onSelectFilter: (com.cadnative.firevisioniptv.presentation.model.GuideFilter) -> Unit,
    onVisibleRangeChanged: (first: Int, last: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf<GuideFocusedProgram?>(null) }
    // Advance the clock so the now-line and live state track real time while the guide is open,
    // instead of freezing at the instant the screen loaded.
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = Instant.now()
        }
    }

    ScreenScaffold(
        title = "Guide",
        modifier = modifier,
        trailing = {
            GuideHeaderDetail(
                focused = focused,
                timelineUnavailable = state.timelineUnavailable
            )
        },
        belowHeader = {
            GuideFilterBar(
                categories = state.categories,
                selectedFilter = state.selectedFilter,
                hasFavorites = state.hasFavorites,
                onSelectFilter = onSelectFilter
            )
        }
    ) {
        GuideGrid(
            rows = state.rows,
            windowStart = state.windowStart,
            windowEnd = state.windowEnd,
            now = now,
            onProgramFocused = { focused = it },
            onProgramSelected = onProgramSelected,
            onChannelSelected = onChannelSelected,
            onVisibleRangeChanged = onVisibleRangeChanged,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Focused-program details in the header band's trailing slot: [live dot +]
 * title over time range · channel, right-aligned. The description doesn't fit
 * the fixed band and is dropped — the trade for giving the old detail panel's
 * height to the grid. Passive and never focusable.
 */
@Composable
private fun GuideHeaderDetail(
    focused: GuideFocusedProgram?,
    timelineUnavailable: Boolean
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600

    if (timelineUnavailable) {
        // With no timeline there are no program cells to focus — show the
        // notice pill instead of the (always empty) program details.
        Text(
            text = "No program schedule available — showing channels only",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(ShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
        )
        return
    }

    if (focused == null) {
        Text(
            text = "Highlight a program for details",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val program = focused.program
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space1)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            if (program.isLive) {
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = "Live now",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(Dimens.IconSmall)
                )
            }
            Text(
                text = program.title,
                style = if (isCompact) MaterialTheme.typography.labelMedium
                else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = buildTimeRange(program.startTime, program.endTime) + " · " + focused.channelName,
            style = if (isCompact) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildTimeRange(start: Instant, end: Instant): String {
    val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(0)
    return "${formatSlotLabel(start)} – ${formatSlotLabel(end)}  (${minutes} min)"
}
