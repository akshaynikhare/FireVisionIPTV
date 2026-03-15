package com.cadnative.firevisioniptv.domain.model

/**
 * Domain model representing the playback state of a channel.
 * 
 * This model tracks the current playback position and status,
 * allowing users to resume playback from where they left off.
 */
data class PlaybackState(
    val channelId: String,
    val position: Long,
    val duration: Long,
    val isPlaying: Boolean
)
