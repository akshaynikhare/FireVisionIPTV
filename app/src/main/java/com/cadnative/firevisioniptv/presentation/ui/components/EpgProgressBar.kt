package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ProgressShape = RoundedCornerShape(1.5.dp)

private val epgTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "21:00-21:30" for a program, in the device time zone. */
fun formatEpgTimeRange(program: EpgProgram): String {
    val zone = ZoneId.systemDefault()
    val start = program.startTime.atZone(zone).format(epgTimeFormatter)
    val end = program.endTime.atZone(zone).format(epgTimeFormatter)
    return "$start-$end"
}

/** Current time, recomputed once per minute — no continuous animation. */
@Composable
fun rememberMinuteTicker(): Long {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L)
        }
    }
    return nowMillis
}

/**
 * Thin amber "how far into the live program" bar. Shared by the player info
 * bar, channel overlay cards, and the mobile portrait channel/schedule lists.
 */
@Composable
fun EpgProgressBar(
    startMs: Long,
    endMs: Long,
    modifier: Modifier = Modifier,
    nowMillis: Long = rememberMinuteTicker()
) {
    val fraction = if (endMs <= startMs) 0f
    else ((nowMillis - startMs).toFloat() / (endMs - startMs)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.EpgProgressHeight)
            .background(OnVideo.copy(alpha = 0.24f), ProgressShape)
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Amber, ProgressShape)
            )
        }
    }
}

@Composable
fun EpgProgressBar(program: EpgProgram, modifier: Modifier = Modifier) {
    EpgProgressBar(
        startMs = program.startTime.toEpochMilli(),
        endMs = program.endTime.toEpochMilli(),
        modifier = modifier
    )
}
