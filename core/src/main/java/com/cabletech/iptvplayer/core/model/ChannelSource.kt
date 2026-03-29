package com.cabletech.iptvplayer.core.model

/**
 * Represents a user-configured M3U channel source (URL or local file path).
 */
data class ChannelSource(
    val id: String,
    val name: String,
    val url: String,
    val lastRefreshedAt: Long? = null,
    val isEnabled: Boolean = true,
)
