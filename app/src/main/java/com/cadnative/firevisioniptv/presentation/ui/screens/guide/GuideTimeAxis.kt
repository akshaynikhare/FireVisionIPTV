package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Minutes between vertical tick labels on the time axis. */
internal const val GUIDE_TICK_MINUTES = 30L

private val hourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** Local-time "h:mm a" label for [instant]. */
internal fun formatSlotLabel(instant: Instant): String =
    hourFormatter.format(instant.atZone(ZoneId.systemDefault()))

/** Horizontal dp offset of [instant] from [windowStart], at [Dimens.GuideMinuteWidth] per minute. */
internal fun timeToDp(instant: Instant, windowStart: Instant): Dp {
    val minutes = Duration.between(windowStart, instant).toMinutes()
    return Dimens.GuideMinuteWidth * minutes.toInt()
}

/** Total pixel width of the whole [windowStart, windowEnd] axis. */
internal fun axisWidth(windowStart: Instant, windowEnd: Instant): Dp =
    timeToDp(windowEnd, windowStart)

/**
 * The scrolling time-axis strip: a tick label every [GUIDE_TICK_MINUTES] positioned
 * absolutely against the same minute→dp scale the grid uses, so cells stay aligned.
 */
@Composable
internal fun GuideTimeAxis(
    windowStart: Instant,
    windowEnd: Instant,
    modifier: Modifier = Modifier
) {
    val totalWidth = axisWidth(windowStart, windowEnd)
    val tickStep = Duration.ofMinutes(GUIDE_TICK_MINUTES)

    Box(
        modifier = modifier
            .requiredWidth(totalWidth)
            .height(Dimens.GuideTimelineHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        var tick = windowStart
        while (!tick.isAfter(windowEnd)) {
            val x = timeToDp(tick, windowStart)
            Text(
                text = formatSlotLabel(tick),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = x + Dimens.GuideTickLabelInset)
            )
            tick = tick.plus(tickStep)
        }
    }
}

/** Sizing helper: the on-screen width of a program clamped to the visible window. */
internal fun programWidthDp(
    start: Instant,
    end: Instant,
    windowStart: Instant,
    windowEnd: Instant
): Dp {
    val clampedStart = if (start.isBefore(windowStart)) windowStart else start
    val clampedEnd = if (end.isAfter(windowEnd)) windowEnd else end
    val minutes = Duration.between(clampedStart, clampedEnd).toMinutes().coerceAtLeast(0)
    val raw = Dimens.GuideMinuteWidth * minutes.toInt()
    return if (raw < Dimens.GuideCellMinWidth) Dimens.GuideCellMinWidth else raw
}
