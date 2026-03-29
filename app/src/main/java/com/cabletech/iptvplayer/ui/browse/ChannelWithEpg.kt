package com.cabletech.iptvplayer.ui.browse

import com.cabletech.iptvplayer.core.model.Channel
import com.cabletech.iptvplayer.epg.NowNextInfo

/**
 * Combines a [Channel] with its optional EPG now/next information for display in the browse UI.
 */
data class ChannelWithEpg(
    val channel: Channel,
    val nowNext: NowNextInfo? = null,
)
