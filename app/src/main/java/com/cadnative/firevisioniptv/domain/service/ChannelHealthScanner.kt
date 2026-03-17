package com.cadnative.firevisioniptv.domain.service

import android.util.Log
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.data.source.local.entity.ChannelHealthEntity
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.domain.model.ChannelHealthStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(
    val scanned: Int = 0,
    val total: Int = 0,
    val isScanning: Boolean = false
)

@Singleton
class ChannelHealthScanner @Inject constructor(
    private val channelHealthDao: ChannelHealthDao,
    private val channelDao: ChannelDao,
    private val thumbnailExtractor: ChannelThumbnailExtractor,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "HealthScanner"
        private const val BATCH_SIZE = 4
        private const val CONNECT_TIMEOUT_SECONDS = 6L
        private const val READ_TIMEOUT_SECONDS = 6L
        private const val THUMBNAIL_DELAY_MS = 5L * 60L * 1000L  // 5 minutes
        private const val COOLDOWN_MS = 30L * 60L * 1000L         // 30 minutes
    }

    private val scanClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private var scanJob: Job? = null
    private val scanMutex = Mutex()
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + dispatcher)

    /**
     * Loop: health scan → 5 min → thumbnail extraction (ONLINE only) → 30 min → repeat
     */
    fun startAutoScan() {
        scope.launch {
            scanMutex.withLock {
                if (scanJob?.isActive == true) return@launch
                scanJob = scope.launch scanLoop@{
            Log.d(TAG, "Auto scan started")
            while (isActive) {
                // Phase 1: Health scan
                runFullScan()

                // Phase 2: 5 min cooldown before thumbnail extraction
                Log.d(TAG, "Health scan done, waiting 5min before thumbnail extraction")
                delay(THUMBNAIL_DELAY_MS)

                // Phase 3: Extract thumbnails for ONLINE channels
                if (isActive) {
                    val count = thumbnailExtractor.extractThumbnails()
                    Log.d(TAG, "Extracted $count thumbnails, cooldown ${COOLDOWN_MS / 60000}min")
                }

                // Phase 4: Main cooldown before next health scan cycle
                delay(COOLDOWN_MS)
            }
                }
            }
        }
    }

    fun triggerManualScan() {
        scope.launch {
            scanMutex.withLock {
                scanJob?.cancel()
                scanJob = scope.launch {
            Log.d(TAG, "Manual scan triggered — incremental rescan")
            runFullScan()

            // Resume auto cycle
            while (isActive) {
                Log.d(TAG, "Waiting 5min for thumbnail extraction")
                delay(THUMBNAIL_DELAY_MS)

                if (isActive) {
                    thumbnailExtractor.extractThumbnails()
                }

                Log.d(TAG, "Cooldown ${COOLDOWN_MS / 60000}min before next health scan")
                delay(COOLDOWN_MS)

                runFullScan()
            }
                }
            }
        }
    }

    fun stopScan() {
        scope.launch {
            scanMutex.withLock {
                scanJob?.cancel()
                scanJob = null
                _scanProgress.value = _scanProgress.value.copy(isScanning = false)
                Log.d(TAG, "Scan stopped")
            }
        }
    }

    fun destroy() {
        stopScan()
        supervisorJob.cancel()
        scanClient.dispatcher.executorService.shutdown()
        scanClient.connectionPool.evictAll()
        Log.d(TAG, "Scanner destroyed")
    }

    private suspend fun runFullScan() {
        channelHealthDao.cleanupOrphaned()

        val allChannelIds = channelHealthDao.getAllChannelIdsByPriority()
        val total = allChannelIds.size
        if (total == 0) return

        _scanProgress.value = ScanProgress(scanned = 0, total = total, isScanning = true)
        Log.d(TAG, "Starting scan of $total channels in batches of $BATCH_SIZE")

        var scannedCount = 0
        val batches = allChannelIds.chunked(BATCH_SIZE)

        for (batch in batches) {
            if (scanJob?.isActive != true) break

            // Mark batch as CHECKING (preserves existing thumbnailPath)
            for (id in batch) {
                channelHealthDao.upsertPreservingThumbnail(
                    channelId = id,
                    status = ChannelHealthStatus.CHECKING.name,
                    lastCheckedAt = System.currentTimeMillis(),
                    responseTimeMs = null,
                    errorMessage = null
                )
            }

            // Get stream URLs for this batch
            val channelUrls = withContext(dispatcher) {
                batch.mapNotNull { id ->
                    val entity = channelDao.getChannelByIdSync(id)
                    entity?.let { id to it.streamUrl }
                }
            }

            // Check streams concurrently within the batch
            val healthResults = coroutineScope {
                channelUrls.map { (id, url) ->
                    async { checkStream(id, url) }
                }.awaitAll()
            }

            // Save results (preserving existing thumbnailPaths)
            for (result in healthResults) {
                channelHealthDao.upsertPreservingThumbnail(
                    channelId = result.channelId,
                    status = result.status,
                    lastCheckedAt = result.lastCheckedAt,
                    responseTimeMs = result.responseTimeMs,
                    errorMessage = result.errorMessage
                )
            }

            scannedCount += batch.size
            _scanProgress.value = ScanProgress(
                scanned = scannedCount.coerceAtMost(total),
                total = total,
                isScanning = true
            )
        }

        _scanProgress.value = _scanProgress.value.copy(isScanning = false)
        Log.d(TAG, "Scan complete: $scannedCount/$total channels checked")
    }

    private suspend fun checkStream(channelId: String, streamUrl: String): ChannelHealthEntity {
        if (streamUrl.isBlank() || !streamUrl.startsWith("http", ignoreCase = true)) {
            return ChannelHealthEntity(
                channelId = channelId,
                status = ChannelHealthStatus.OFFLINE.name,
                lastCheckedAt = System.currentTimeMillis(),
                errorMessage = "Invalid URL"
            )
        }

        return withContext(dispatcher) {
            val startTime = System.currentTimeMillis()
            try {
                val isHls = streamUrl.contains(".m3u8", ignoreCase = true)

                if (isHls) {
                    checkHlsStream(channelId, streamUrl, startTime)
                } else {
                    checkGenericStream(channelId, streamUrl, startTime)
                }
            } catch (e: Exception) {
                ChannelHealthEntity(
                    channelId = channelId,
                    status = ChannelHealthStatus.OFFLINE.name,
                    lastCheckedAt = System.currentTimeMillis(),
                    responseTimeMs = System.currentTimeMillis() - startTime,
                    errorMessage = e.message?.take(200)
                )
            }
        }
    }

    private fun checkHlsStream(
        channelId: String,
        streamUrl: String,
        startTime: Long
    ): ChannelHealthEntity {
        val request = Request.Builder().url(streamUrl).get().build()
        val response = scanClient.newCall(request).execute()
        val responseTime = System.currentTimeMillis() - startTime

        return response.use { resp ->
            if (!resp.isSuccessful) {
                return@use ChannelHealthEntity(
                    channelId = channelId,
                    status = ChannelHealthStatus.OFFLINE.name,
                    lastCheckedAt = System.currentTimeMillis(),
                    responseTimeMs = responseTime,
                    errorMessage = "HTTP ${resp.code}"
                )
            }

            // Read first 4KB to validate HLS manifest
            val peek = resp.body?.let { body ->
                body.source().use { source ->
                    val buffer = okio.Buffer()
                    source.read(buffer, 4096)
                    buffer.readUtf8()
                }
            } ?: ""

            val isValid = peek.contains("#EXTM3U") ||
                    peek.contains("#EXT-X-STREAM-INF") ||
                    peek.contains("#EXTINF")

            ChannelHealthEntity(
                channelId = channelId,
                status = if (isValid) ChannelHealthStatus.ONLINE.name else ChannelHealthStatus.OFFLINE.name,
                lastCheckedAt = System.currentTimeMillis(),
                responseTimeMs = responseTime,
                errorMessage = if (!isValid) "Invalid HLS manifest" else null
            )
        }
    }

    private fun checkGenericStream(
        channelId: String,
        streamUrl: String,
        startTime: Long
    ): ChannelHealthEntity {
        // Try HEAD first, fall back to GET with range
        val headRequest = Request.Builder().url(streamUrl).head().build()
        return try {
            val response = scanClient.newCall(headRequest).execute()
            val responseTime = System.currentTimeMillis() - startTime

            response.use { resp ->
                ChannelHealthEntity(
                    channelId = channelId,
                    status = if (resp.isSuccessful) ChannelHealthStatus.ONLINE.name
                    else ChannelHealthStatus.OFFLINE.name,
                    lastCheckedAt = System.currentTimeMillis(),
                    responseTimeMs = responseTime,
                    errorMessage = if (!resp.isSuccessful) "HTTP ${resp.code}" else null
                )
            }
        } catch (e: Exception) {
            // HEAD not supported, try GET with range header
            val getRequest = Request.Builder()
                .url(streamUrl)
                .header("Range", "bytes=0-1023")
                .get()
                .build()

            val response = scanClient.newCall(getRequest).execute()
            val responseTime = System.currentTimeMillis() - startTime

            response.use { resp ->
                ChannelHealthEntity(
                    channelId = channelId,
                    status = if (resp.isSuccessful || resp.code == 206) ChannelHealthStatus.ONLINE.name
                    else ChannelHealthStatus.OFFLINE.name,
                    lastCheckedAt = System.currentTimeMillis(),
                    responseTimeMs = responseTime,
                    errorMessage = if (!resp.isSuccessful && resp.code != 206) "HTTP ${resp.code}" else null
                )
            }
        }
    }
}
