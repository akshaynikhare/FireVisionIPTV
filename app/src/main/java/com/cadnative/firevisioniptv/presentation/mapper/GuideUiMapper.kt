package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.domain.model.Channel
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.GuideProgramUiModel
import com.cadnative.firevisioniptv.presentation.model.GuideRowUiModel
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuideUiMapper @Inject constructor() {

    /**
     * Builds Guide rows for [channels], clamping each channel's programs to the
     * [windowStart, windowEnd] axis. Row order matches [channels]; the row number
     * is the 1-based position so users see a stable channel numbering.
     */
    fun toRows(
        channels: List<Channel>,
        programsByTvgId: Map<String, List<EpgProgram>>,
        windowStart: Instant,
        windowEnd: Instant,
        now: Instant
    ): List<GuideRowUiModel> {
        return channels.mapIndexed { index, channel ->
            val programs = channel.tvgId
                ?.let { programsByTvgId[it] }
                .orEmpty()
                .map { it.toUiModel(now) }
            GuideRowUiModel(
                channelId = channel.id,
                channelName = channel.name,
                channelNumber = index + 1,
                logoUrl = channel.logoUrl,
                category = channel.category,
                programs = programs
            )
        }
    }

    private fun EpgProgram.toUiModel(now: Instant): GuideProgramUiModel =
        GuideProgramUiModel(
            id = "$channelEpgId-${startTime.epochSecond}",
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            isLive = startTime <= now && endTime > now
        )
}
