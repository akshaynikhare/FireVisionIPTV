package com.cabletech.iptvplayer.epg

/**
 * Domain model for a single EPG (Electronic Programme Guide) entry.
 *
 * @param channelId  Corresponds to Channel.tvgId for lookup.
 * @param title      Programme title.
 * @param startUtcMs Start time in UTC epoch milliseconds.
 * @param stopUtcMs  Stop time in UTC epoch milliseconds.
 * @param description Optional programme synopsis.
 */
data class EpgProgramme(
    val channelId: String,
    val title: String,
    val startUtcMs: Long,
    val stopUtcMs: Long,
    val description: String? = null,
)
