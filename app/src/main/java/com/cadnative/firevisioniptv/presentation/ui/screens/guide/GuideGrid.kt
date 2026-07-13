package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.GuideFocusedProgram
import com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel
import com.cadnative.firevisioniptv.presentation.model.GuideRowUiModel
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import java.time.Instant

/**
 * The scrollable channels × time grid: a sticky channel column on the left, a shared
 * horizontally-scrolling program lane per row, and a vertical "now" line overlaid on
 * the lanes. Horizontal scroll is shared between the time axis (above) and every row
 * so program cells stay aligned with the axis ticks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GuideGrid(
    rows: List<GuideRowUiModel>,
    windowStart: Instant,
    windowEnd: Instant,
    now: Instant,
    onProgramFocused: (GuideFocusedProgram) -> Unit,
    onProgramSelected: (channelId: String, program: GuideProgramUiModel) -> Unit,
    onChannelSelected: (channelId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val laneWidth = axisWidth(windowStart, windowEnd)
    val nowOffset = timeToDp(now, windowStart)
    // Land focus on the first channel so the D-pad has an entry point even when
    // no EPG programs exist (all-gap rows have no other focusable target).
    val firstChannelFocus = remember { FocusRequester() }
    LaunchedEffect(rows.isNotEmpty()) {
        if (rows.isNotEmpty()) runCatching { firstChannelFocus.requestFocus() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Time axis header (channel-column spacer + scrolling ticks) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(Dimens.GuideChannelColumnWidth)
                    .height(Dimens.GuideTimelineHeight)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                GuideTimeAxis(windowStart = windowStart, windowEnd = windowEnd)
            }
        }

        // ── Rows ──
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(rows, key = { _, r -> r.channelId }) { index, row ->
                    GuideRow(
                        row = row,
                        rowIndex = index,
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        laneWidth = laneWidth,
                        horizontalScroll = horizontalScroll,
                        channelFocusRequester = if (index == 0) firstChannelFocus else null,
                        onProgramFocused = onProgramFocused,
                        onProgramSelected = { program -> onProgramSelected(row.channelId, program) },
                        onChannelSelected = { onChannelSelected(row.channelId) }
                    )
                }
            }

            // ── "Now" indicator — overlaid on the lanes, moves with the shared
            // horizontal scroll. Hosted in a matching horizontalScroll container
            // so its position tracks the lanes exactly. ──
            if (!now.isBefore(windowStart) && now.isBefore(windowEnd)) {
                Box(
                    modifier = Modifier
                        .padding(start = Dimens.GuideChannelColumnWidth)
                        .fillMaxSize()
                        .horizontalScroll(horizontalScroll, enabled = false)
                ) {
                    // Full-lane-width host so the shared scroll offset applies to the line.
                    Box(modifier = Modifier.requiredWidth(laneWidth).fillMaxHeight()) {
                        GuideNowLine(offset = nowOffset)
                    }
                }
            }
        }
    }
}

/** One channel row: sticky channel cell + horizontally-scrolling program lane. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GuideRow(
    row: GuideRowUiModel,
    rowIndex: Int,
    windowStart: Instant,
    windowEnd: Instant,
    laneWidth: androidx.compose.ui.unit.Dp,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    channelFocusRequester: FocusRequester?,
    onProgramFocused: (GuideFocusedProgram) -> Unit,
    onProgramSelected: (GuideProgramUiModel) -> Unit,
    onChannelSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.GuideRowHeight)
    ) {
        GuideChannelCell(
            name = row.channelName,
            number = row.channelNumber,
            logoUrl = row.logoUrl,
            category = row.category,
            onSelected = onChannelSelected,
            focusRequester = channelFocusRequester,
            modifier = Modifier.width(Dimens.GuideChannelColumnWidth)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(horizontalScroll)
                .requiredWidth(laneWidth)
                .padding(vertical = Dimens.GuideCellGap),
            horizontalArrangement = Arrangement.Start
        ) {
            if (row.programs.isEmpty()) {
                GuideGapCell(width = laneWidth)
            } else {
                // Leading spacer aligns the first cell to its real start on the axis.
                val leadDp = timeToDp(row.programs.first().startTime, windowStart)
                if (leadDp > 0.dp) {
                    Box(modifier = Modifier.width(leadDp).fillMaxHeight())
                }
                row.programs.forEach { program ->
                    GuideProgramCell(
                        program = program,
                        width = programWidthDp(program.startTime, program.endTime, windowStart, windowEnd),
                        category = row.category,
                        onFocused = {
                            onProgramFocused(
                                GuideFocusedProgram(
                                    rowIndex = rowIndex,
                                    program = program,
                                    channelName = row.channelName
                                )
                            )
                        },
                        onSelected = { onProgramSelected(program) }
                    )
                }
            }
        }
    }
}

/**
 * Focusable channel cell — the guide's primary navigation target. Up/Down moves
 * between channels; Center tunes the channel (works with or without EPG data).
 */
@Composable
private fun GuideChannelCell(
    name: String,
    number: Int,
    logoUrl: String?,
    category: String,
    onSelected: () -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusVisuals(focused = focused, shape = shape, glowColor = categoryColor(category))
            .clip(shape)
            .background(
                if (focused) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelected() },
        contentAlignment = Alignment.CenterStart
    ) {
        GuideChannelHeader(
            name = name,
            number = number,
            logoUrl = logoUrl,
            category = category
        )
    }
}
