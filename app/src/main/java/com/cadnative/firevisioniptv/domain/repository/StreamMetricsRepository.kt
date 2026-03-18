package com.cadnative.firevisioniptv.domain.repository

import com.cadnative.firevisioniptv.data.model.Result

interface StreamMetricsRepository {

    suspend fun reportStreamDead(channelId: String, errorMessage: String?): Result<Unit>

    suspend fun reportStreamAlive(channelId: String): Result<Unit>

    suspend fun reportStreamUnresponsive(channelId: String): Result<Unit>

    suspend fun reportStreamPlay(channelId: String): Result<Unit>

    suspend fun syncHealthResults(results: List<HealthSyncEntry>): Result<Unit>
}

data class HealthSyncEntry(
    val channelId: String,
    val status: String,
    val responseTimeMs: Long?,
    val timestamp: Long
)
