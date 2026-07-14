package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.GuideFocusedProgram
import com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel
import com.cadnative.firevisioniptv.presentation.model.GuideRowUiModel
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FOCUS
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideCellFocusEnd
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideCellFocusStart
import com.cadnative.firevisioniptv.presentation.ui.theme.GuideRowWash
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The scrollable channels × time grid: a sticky channel column on the left, a shared
 * horizontally-scrolling program lane per row, and a vertical "now" line overlaid on
 * the lanes. Horizontal scroll is shared between the time axis (above) and every row
 * so program cells stay aligned with the axis ticks.
 *
 * Rows hydrate lazily: [onVisibleRangeChanged] reports the viewport so the ViewModel can
 * load only the programs in view, keeping the guide responsive on very large channel
 * lists. Back snaps the timeline to "now" before it exits the screen.
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
    onVisibleRangeChanged: (first: Int, last: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val channelColumnWidth =
        if (isCompact) Dimens.GuideChannelColumnWidthMobile else Dimens.GuideChannelColumnWidth
    val rowHeight = if (isCompact) Dimens.GuideRowHeightMobile else Dimens.GuideRowHeight
    val timelineHeight =
        if (isCompact) Dimens.GuideTimelineHeightMobile else Dimens.GuideTimelineHeight
    val minuteWidth = if (isCompact) Dimens.GuideMinuteWidthMobile else Dimens.GuideMinuteWidth

    val horizontalScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val laneWidth = axisWidth(windowStart, windowEnd, minuteWidth)
    val nowOffset = timeToDp(now, windowStart, minuteWidth)

    // Back snaps the timeline to "now" (scroll origin) before letting the screen exit.
    BackHandler(enabled = horizontalScroll.value > 0) {
        scope.launch {
            if (reduceMotion) horizontalScroll.scrollTo(0) else horizontalScroll.animateScrollTo(0)
        }
    }

    // Report the viewport so the ViewModel hydrates only visible rows. collectLatest
    // debounces flings (each new range cancels the previous pending emit).
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val first = listState.firstVisibleItemIndex
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: first
            first to last
        }
            .distinctUntilChanged()
            .collectLatest { (first, last) ->
                delay(100)
                onVisibleRangeChanged(first, last)
            }
    }

    // Land focus on the first channel so the D-pad has an entry point even when a row
    // has no EPG. Re-fires when the first channel changes (e.g. after a filter switch).
    val firstChannelFocus = remember { FocusRequester() }
    val firstChannelId = rows.firstOrNull()?.channelId
    LaunchedEffect(firstChannelId) {
        if (firstChannelId != null) runCatching { firstChannelFocus.requestFocus() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Time axis header (channel-column spacer + scrolling ticks) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(channelColumnWidth)
                    .height(timelineHeight)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                GuideTimeAxis(
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    minuteWidth = minuteWidth,
                    timelineHeight = timelineHeight
                )
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
                        rowHeight = rowHeight,
                        channelColumnWidth = channelColumnWidth,
                        minuteWidth = minuteWidth,
                        isCompact = isCompact,
                        reduceMotion = reduceMotion,
                        horizontalScroll = horizontalScroll,
                        channelFocusRequester = if (index == 0) firstChannelFocus else null,
                        onProgramFocused = onProgramFocused,
                        onProgramSelected = { program -> onProgramSelected(row.channelId, program) },
                        onChannelSelected = { onChannelSelected(row.channelId) }
                    )
                }
            }

            // ── "Now" indicator — overlaid on the lanes, tracks the shared horizontal scroll. ──
            if (!now.isBefore(windowStart) && now.isBefore(windowEnd)) {
                Box(
                    modifier = Modifier
                        .padding(start = channelColumnWidth)
                        .fillMaxSize()
                        .horizontalScroll(horizontalScroll, enabled = false)
                ) {
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
    laneWidth: Dp,
    rowHeight: Dp,
    channelColumnWidth: Dp,
    minuteWidth: Dp,
    isCompact: Boolean,
    reduceMotion: Boolean,
    horizontalScroll: ScrollState,
    channelFocusRequester: FocusRequester?,
    onProgramFocused: (GuideFocusedProgram) -> Unit,
    onProgramSelected: (GuideProgramUiModel) -> Unit,
    onChannelSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rowFocused by remember { mutableStateOf(false) }
    val washAlpha by animateFloatAsState(
        targetValue = if (rowFocused) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_FOCUS, easing = EaseOutQuart),
        label = "guideRowWash"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .onFocusChanged { rowFocused = it.hasFocus }
    ) {
        GuideChannelCell(
            name = row.channelName,
            number = row.channelNumber,
            logoUrl = row.logoUrl,
            category = row.category,
            isCompact = isCompact,
            rowFocused = rowFocused,
            onSelected = onChannelSelected,
            focusRequester = channelFocusRequester,
            modifier = Modifier.width(channelColumnWidth)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(horizontalScroll)
                .requiredWidth(laneWidth)
                .drawBehind {
                    if (washAlpha > 0f) {
                        drawRect(
                            brush = Brush.horizontalGradient(listOf(GuideRowWash, Color.Transparent)),
                            alpha = washAlpha
                        )
                    }
                }
                .padding(vertical = Dimens.GuideCellGap),
            horizontalArrangement = Arrangement.Start
        ) {
            when {
                !row.isHydrated ->
                    GuideGapCell(width = laneWidth, isCompact = isCompact, label = "Loading…")

                row.programs.isEmpty() ->
                    GuideGapCell(width = laneWidth, isCompact = isCompact)

                else -> {
                    // Leading spacer aligns the first cell to its real start on the axis.
                    val leadDp = timeToDp(row.programs.first().startTime, windowStart, minuteWidth)
                    if (leadDp > 0.dp) {
                        Box(modifier = Modifier.width(leadDp).fillMaxHeight())
                    }
                    row.programs.forEachIndexed { i, program ->
                        // Insert a spacer for a real schedule gap so each cell sits at its true
                        // time — keeps the "now" line and sticky title aligned with the axis.
                        if (i > 0) {
                            val prev = row.programs[i - 1]
                            val gapDp = timeToDp(program.startTime, windowStart, minuteWidth) -
                                timeToDp(prev.startTime, windowStart, minuteWidth) -
                                programWidthDp(prev.startTime, prev.endTime, windowStart, windowEnd, minuteWidth)
                            if (gapDp > 0.dp) {
                                Box(modifier = Modifier.width(gapDp).fillMaxHeight())
                            }
                        }
                        GuideProgramCell(
                            program = program,
                            width = programWidthDp(
                                program.startTime, program.endTime, windowStart, windowEnd, minuteWidth
                            ),
                            laneOffset = timeToDp(program.startTime, windowStart, minuteWidth)
                                .coerceAtLeast(0.dp),
                            scrollState = horizontalScroll,
                            category = row.category,
                            isCompact = isCompact,
                            rowFocused = rowFocused,
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
}

/**
 * Focusable channel cell — the guide's primary navigation target. Up/Down moves
 * between channels; Center tunes the channel (works with or without EPG data). When
 * its row holds focus the cell lifts and shows a category accent bar so the active row
 * is unmistakable; when the cell itself is focused it gains the amber gradient + border.
 */
@Composable
private fun GuideChannelCell(
    name: String,
    number: Int,
    logoUrl: String?,
    category: String,
    isCompact: Boolean,
    rowFocused: Boolean,
    onSelected: () -> Unit,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = ShapeMedium
    val catColor = categoryColor(category)
    val background = when {
        focused -> Brush.horizontalGradient(listOf(GuideCellFocusStart, GuideCellFocusEnd))
        rowFocused -> SolidColor(MaterialTheme.colorScheme.surfaceVariant)
        else -> SolidColor(MaterialTheme.colorScheme.surface)
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            // No elevation glow (see GuideProgramCell): the row clips each cell to its exact
            // height, which shears a drop-shadow into a hard band. Border + gradient are the cue.
            .tvFocusVisuals(
                focused = focused,
                shape = shape,
                glowColor = catColor,
                restingElevation = 0.dp,
                focusedElevation = 0.dp
            )
            .clip(shape)
            .background(background)
            .then(
                if (focused) Modifier.border(Dimens.GuideFocusBorderWidth, FocusBorder, shape)
                else Modifier
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelected() },
        contentAlignment = Alignment.CenterStart
    ) {
        if (rowFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(Dimens.GuideRowAccentWidth)
                    .fillMaxHeight()
                    .background(catColor)
            )
        }
        GuideChannelHeader(
            name = name,
            number = number,
            logoUrl = logoUrl,
            category = category,
            isCompact = isCompact
        )
    }
}
