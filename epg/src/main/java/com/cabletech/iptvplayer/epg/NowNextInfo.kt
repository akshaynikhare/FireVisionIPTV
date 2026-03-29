package com.cabletech.iptvplayer.epg

/**
 * Convenience holder for the currently-airing and next-up programmes for a channel.
 * Used by the browse UI to display now/next info on channel cards.
 */
data class NowNextInfo(
    val now: EpgProgramme?,
    val next: EpgProgramme?,
)
