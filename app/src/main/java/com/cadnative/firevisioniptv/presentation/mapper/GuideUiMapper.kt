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
     * Lightweight rows for every channel with no programs yet. Row order matches
     * [channels] and the row number is the 1-based position, so channel numbering is
     * stable no matter which filter is active. Programs are hydrated later, per row,
     * as the row scrolls into view.
     */
    fun toSkeletonRows(channels: List<Channel>): List<GuideRowUiModel> =
        channels.mapIndexed { index, channel ->
            GuideRowUiModel(
                channelId = channel.id,
                channelName = channel.name,
                channelNumber = index + 1,
                logoUrl = channel.logoUrl,
                category = channel.category,
                programs = emptyList(),
                isHydrated = false
            )
        }

    /** Map one channel's raw EPG programs to UI models, clamped implicitly by the caller's window. */
    fun toPrograms(programs: List<EpgProgram>, now: Instant): List<GuideProgramUiModel> =
        programs.map { it.toUiModel(now) }

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
