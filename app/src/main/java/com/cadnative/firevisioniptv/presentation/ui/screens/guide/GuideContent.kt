package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.GuideFocusedProgram
import com.cadnative.firevisioniptv.presentation.model.GuideUiState
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * Guide layout: a detail header for the focused program on top, the channels × time
 * grid below. The header updates as D-pad focus moves between program cells.
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

    Column(modifier = modifier.fillMaxSize()) {
        GuideDetailPanel(focused = focused)

        GuideFilterBar(
            categories = state.categories,
            selectedFilter = state.selectedFilter,
            hasFavorites = state.hasFavorites,
            onSelectFilter = onSelectFilter
        )

        if (state.timelineUnavailable) {
            GuideTimelineNotice()
        }

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

/** Focused-program detail: title, time range, and description (when the server sends one). */
@Composable
private fun GuideDetailPanel(
    focused: GuideFocusedProgram?,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCompact) Dimens.GuideDetailPanelHeightMobile else Dimens.GuideDetailPanelHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile
                else Dimens.ScreenPaddingHorizontalTv,
                vertical = Dimens.Space2
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (focused == null) {
            Text(
                text = "Program Guide",
                style = if (isCompact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            return@Box
        }

        val program = focused.program
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
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
                    style = MaterialTheme.typography.titleMedium,
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
            program.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = if (isCompact) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Shown when channels loaded but no program timeline was available. */
@Composable
private fun GuideTimelineNotice(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPaddingHorizontalTv, vertical = Dimens.Space2)
            .clip(ShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
    ) {
        Text(
            text = "No program schedule available — showing channels only",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildTimeRange(start: Instant, end: Instant): String {
    val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(0)
    return "${formatSlotLabel(start)} – ${formatSlotLabel(end)}  (${minutes} min)"
}
