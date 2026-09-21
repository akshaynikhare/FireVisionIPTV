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
import java.util.concurrent.ConcurrentHashMap

/** Minutes between vertical tick labels on the time axis. */
internal const val GUIDE_TICK_MINUTES = 30L

// Cached rather than rebuilt per label — this runs for every tick of every axis
// redraw, and below API 26 it resolves through desugared java.time, which is not
// free. Keyed by locale so switching device language still takes effect.
private val slotFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()

// The AM/PM marker comes from the device's CLDR data, which on older API levels is
// several revisions behind what the pattern expects. A wrong marker is cosmetic; an
// exception thrown per tick inside the axis is a crash loop — so fall back to 24h.
private val slotFormatterFallback = DateTimeFormatter.ofPattern("H:mm")

/** Local-time "h:mm a" label for [instant]. */
internal fun formatSlotLabel(instant: Instant): String {
    val local = instant.atZone(ZoneId.systemDefault())
    val locale = Locale.getDefault()
    val formatter = slotFormatters.getOrPut(locale) {
        DateTimeFormatter.ofPattern("h:mm a", locale)
    }
    return runCatching { formatter.format(local) }
        .getOrElse { slotFormatterFallback.format(local) }
}

/**
 * Horizontal dp offset of [instant] from [windowStart], at [minuteWidth] per minute.
 * [minuteWidth] is passed by callers so the axis and grid share one compact-aware scale.
 */
internal fun timeToDp(instant: Instant, windowStart: Instant, minuteWidth: Dp): Dp {
    val minutes = Duration.between(windowStart, instant).toMinutes()
    return minuteWidth * minutes.toInt()
}

/** Total pixel width of the whole [windowStart, windowEnd] axis. */
internal fun axisWidth(windowStart: Instant, windowEnd: Instant, minuteWidth: Dp): Dp =
    timeToDp(windowEnd, windowStart, minuteWidth)

/**
 * The scrolling time-axis strip: a tick label every [GUIDE_TICK_MINUTES] positioned
 * absolutely against the same minute→dp scale the grid uses, so cells stay aligned.
 */
@Composable
internal fun GuideTimeAxis(
    windowStart: Instant,
    windowEnd: Instant,
    minuteWidth: Dp,
    timelineHeight: Dp,
    modifier: Modifier = Modifier
) {
    val totalWidth = axisWidth(windowStart, windowEnd, minuteWidth)
    val tickStep = Duration.ofMinutes(GUIDE_TICK_MINUTES)

    Box(
        modifier = modifier
            .requiredWidth(totalWidth)
            .height(timelineHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        var tick = windowStart
        while (!tick.isAfter(windowEnd)) {
            val x = timeToDp(tick, windowStart, minuteWidth)
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
    windowEnd: Instant,
    minuteWidth: Dp
): Dp {
    val clampedStart = if (start.isBefore(windowStart)) windowStart else start
    val clampedEnd = if (end.isAfter(windowEnd)) windowEnd else end
    val minutes = Duration.between(clampedStart, clampedEnd).toMinutes().coerceAtLeast(0)
    val raw = minuteWidth * minutes.toInt()
    return if (raw < Dimens.GuideCellMinWidth) Dimens.GuideCellMinWidth else raw
}
