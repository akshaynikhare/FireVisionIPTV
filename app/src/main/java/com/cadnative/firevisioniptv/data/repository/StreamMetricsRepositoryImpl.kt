package com.cadnative.firevisioniptv.data.repository

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.HealthSyncItem
import com.cadnative.firevisioniptv.data.model.dto.HealthSyncRequest
import com.cadnative.firevisioniptv.data.model.dto.StreamPlayReport
import com.cadnative.firevisioniptv.data.model.dto.StreamStatusReport
import com.cadnative.firevisioniptv.data.source.local.dao.StreamMetricsDao
import com.cadnative.firevisioniptv.data.source.local.entity.StreamMetricsEntity
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.repository.HealthSyncEntry
import com.cadnative.firevisioniptv.domain.repository.StreamMetricsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamMetricsRepositoryImpl @Inject constructor(
    private val streamMetricsDao: StreamMetricsDao,
    private val apiService: FireVisionApiService,
    private val application: Application,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : StreamMetricsRepository {

    companion object {
        private const val TAG = "StreamMetricsRepo"
    }

    override suspend fun reportStreamDead(channelId: String, errorMessage: String?): Result<Unit> =
        withContext(dispatcher) {
            try {
                ensureMetricsRow(channelId)
                val now = System.currentTimeMillis()
                streamMetricsDao.incrementDead(channelId, now)

                // Fire-and-forget API call
                try {
                    apiService.reportStreamStatus(
                        channelId,
                        StreamStatusReport(
                            status = "dead",
                            deviceId = getDeviceId(),
                            timestamp = now,
                            errorMessage = errorMessage
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to report dead status to server: ${e.message}")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun reportStreamAlive(channelId: String): Result<Unit> =
        withContext(dispatcher) {
            try {
                ensureMetricsRow(channelId)
                val now = System.currentTimeMillis()
                streamMetricsDao.incrementAlive(channelId, now)

                try {
                    apiService.reportStreamStatus(
                        channelId,
                        StreamStatusReport(
                            status = "alive",
                            deviceId = getDeviceId(),
                            timestamp = now
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to report alive status to server: ${e.message}")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun reportStreamUnresponsive(channelId: String): Result<Unit> =
        withContext(dispatcher) {
            try {
                ensureMetricsRow(channelId)
                val now = System.currentTimeMillis()
                streamMetricsDao.incrementUnresponsive(channelId, now)

                try {
                    apiService.reportStreamStatus(
                        channelId,
                        StreamStatusReport(
                            status = "unresponsive",
                            deviceId = getDeviceId(),
                            timestamp = now
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to report unresponsive status to server: ${e.message}")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun reportStreamPlay(channelId: String, proxyPlay: Boolean): Result<Unit> =
        withContext(dispatcher) {
            try {
                ensureMetricsRow(channelId)
                val now = System.currentTimeMillis()
                streamMetricsDao.incrementPlay(channelId, now)

                try {
                    apiService.reportStreamPlay(
                        channelId,
                        StreamPlayReport(
                            deviceId = getDeviceId(),
                            timestamp = now,
                            proxyPlay = proxyPlay
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to report play to server: ${e.message}")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    override suspend fun syncHealthResults(results: List<HealthSyncEntry>): Result<Unit> =
        withContext(dispatcher) {
            try {
                // Update local counters
                for (entry in results) {
                    ensureMetricsRow(entry.channelId)
                    when (entry.status.lowercase()) {
                        "alive", "online" -> streamMetricsDao.incrementAlive(entry.channelId, entry.timestamp)
                        "dead", "offline" -> streamMetricsDao.incrementDead(entry.channelId, entry.timestamp)
                        "unresponsive" -> streamMetricsDao.incrementUnresponsive(entry.channelId, entry.timestamp)
                    }
                }

                // Bulk sync to server
                try {
                    val request = HealthSyncRequest(
                        deviceId = getDeviceId(),
                        results = results.map { entry ->
                            HealthSyncItem(
                                channelId = entry.channelId,
                                status = entry.status,
                                responseTimeMs = entry.responseTimeMs,
                                timestamp = entry.timestamp
                            )
                        }
                    )
                    apiService.syncHealthResults(request)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync health results to server: ${e.message}")
                }

                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }

    private suspend fun ensureMetricsRow(channelId: String) {
        if (streamMetricsDao.getByChannelId(channelId) == null) {
            streamMetricsDao.upsert(StreamMetricsEntity(channelId = channelId))
        }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}
