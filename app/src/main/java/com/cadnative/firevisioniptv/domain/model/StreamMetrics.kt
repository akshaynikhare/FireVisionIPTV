package com.cadnative.firevisioniptv.domain.model

data class StreamMetrics(
    val channelId: String,
    val playCount: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val unresponsiveCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val lastDeadAt: Long? = null,
    val lastAliveAt: Long? = null,
    val lastUnresponsiveAt: Long? = null
)
