package com.cadnative.firevisioniptv.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelDao
import com.cadnative.firevisioniptv.data.source.local.dao.ChannelHealthDao
import com.cadnative.firevisioniptv.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelThumbnailExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelHealthDao: ChannelHealthDao,
    private val channelDao: ChannelDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ThumbnailExtractor"
        private const val BATCH_SIZE = 3
        private const val THUMBNAIL_WIDTH = 320
        private const val THUMBNAIL_HEIGHT = 180
        private const val JPEG_QUALITY = 70
        private const val EXTRACT_TIMEOUT_MS = 30_000L
        private val SAFE_FILENAME_REGEX = Regex("[^a-zA-Z0-9._-]")
    }

    // Lightweight client for fetching HLS manifests
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // Persistent storage — survives cache clears
    private val thumbnailDir: File by lazy {
        File(context.filesDir, "thumbnails").also { it.mkdirs() }
    }

    suspend fun extractThumbnails(): Int {
        val onlineIds = channelHealthDao.getOnlineChannelIdsWithoutThumbnail()
        if (onlineIds.isEmpty()) {
            Log.d(TAG, "No ONLINE channels need thumbnails")
            return 0
        }

        Log.d(TAG, "Extracting thumbnails for ${onlineIds.size} online channels")

        var extractedCount = 0
        val batches = onlineIds.chunked(BATCH_SIZE)

        for (batch in batches) {
            val channelUrls = withContext(dispatcher) {
                batch.mapNotNull { id ->
                    channelDao.getChannelByIdSync(id)?.let { id to it.streamUrl }
                }
            }

            val results = coroutineScope {
                channelUrls.map { (id, url) ->
                    async { extractSingleThumbnail(id, url) }
                }.awaitAll()
            }

            extractedCount += results.count { it }
        }

        Log.d(TAG, "Thumbnail extraction complete: $extractedCount/${onlineIds.size} extracted")
        return extractedCount
    }

    /**
     * Save a bitmap as a channel thumbnail. Reusable by both background
     * extraction and the live player capture path.
     */
    suspend fun saveThumbnailBitmap(channelId: String, bitmap: Bitmap): Boolean {
        return withContext(dispatcher) {
            try {
                val scaled = Bitmap.createScaledBitmap(
                    bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true
                )
                try {
                    val safeId = channelId.replace(SAFE_FILENAME_REGEX, "_")
                    val file = File(thumbnailDir, "${safeId}.jpg")
                    file.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    }
                    if (file.exists() && file.length() > 0) {
                        channelHealthDao.updateThumbnailPath(channelId, file.absolutePath)
                        Log.d(TAG, "Thumbnail saved for $channelId from player")
                        true
                    } else {
                        Log.w(TAG, "Thumbnail file invalid after write for $channelId")
                        false
                    }
                } finally {
                    if (scaled !== bitmap) scaled.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save player thumbnail for $channelId", e)
                false
            }
        }
    }

    private suspend fun extractSingleThumbnail(channelId: String, streamUrl: String): Boolean {
        if (streamUrl.isBlank() || !streamUrl.startsWith("http", ignoreCase = true)) {
            return false
        }

        return withContext(dispatcher) {
            val retriever = MediaMetadataRetriever()
            try {
                withTimeout(EXTRACT_TIMEOUT_MS) {
                    // For HLS streams, resolve to a direct segment URL
                    val resolvedUrl = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                        resolveHlsSegmentUrl(streamUrl) ?: run {
                            Log.w(TAG, "Could not resolve HLS segment for $channelId")
                            return@withTimeout false
                        }
                    } else {
                        streamUrl
                    }

                    retriever.setDataSource(resolvedUrl, HashMap<String, String>())
                    val frame = retriever.getFrameAtTime(
                        0,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: return@withTimeout false

                    val scaled = Bitmap.createScaledBitmap(
                        frame, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true
                    )
                    if (scaled !== frame) frame.recycle()

                    try {
                        val safeId = channelId.replace(SAFE_FILENAME_REGEX, "_")
                        val file = File(thumbnailDir, "${safeId}.jpg")
                        file.outputStream().use { out ->
                            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                        }

                        if (file.exists() && file.length() > 0) {
                            channelHealthDao.updateThumbnailPath(channelId, file.absolutePath)
                            Log.d(TAG, "Thumbnail saved for $channelId")
                            true
                        } else {
                            Log.w(TAG, "Thumbnail file invalid after write for $channelId")
                            false
                        }
                    } finally {
                        scaled.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed thumbnail for $channelId: ${e.message}", e)
                false
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to release retriever for $channelId: ${e.message}")
                }
            }
        }
    }

    /**
     * Resolve an HLS .m3u8 manifest to a direct .ts segment URL.
     * Handles both master playlists (with variant streams) and media playlists.
     */
    private fun resolveHlsSegmentUrl(manifestUrl: String): String? {
        return try {
            val body = fetchManifestBody(manifestUrl) ?: return null
            val baseUri = URI(manifestUrl)

            // Master playlist — contains variant stream references
            if (body.contains("#EXT-X-STREAM-INF")) {
                val variantUrl = body.lines()
                    .dropWhile { !it.startsWith("#EXT-X-STREAM-INF") }
                    .drop(1)
                    .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                    ?: return null

                val resolvedVariantUrl = resolveUrl(baseUri, variantUrl)
                return resolveHlsSegmentUrl(resolvedVariantUrl)
            }

            // Media playlist — find the first segment
            val segmentLine = body.lines().firstOrNull { line ->
                line.isNotBlank() && !line.startsWith("#")
            }

            if (segmentLine != null) {
                resolveUrl(baseUri, segmentLine)
            } else {
                Log.w(TAG, "No segment found in HLS playlist: $manifestUrl")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve HLS manifest: $manifestUrl", e)
            null
        }
    }

    private fun fetchManifestBody(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "HLS manifest fetch failed: HTTP ${response.code} for $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "HLS manifest fetch error for $url: ${e.message}")
            null
        }
    }

    private fun resolveUrl(base: URI, relative: String): String {
        val trimmed = relative.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            base.resolve(trimmed).toString()
        }
    }

    suspend fun clearThumbnails() {
        withContext(dispatcher) {
            thumbnailDir.listFiles()?.forEach { it.delete() }
        }
        channelHealthDao.clearAllThumbnailPaths()
        Log.d(TAG, "Thumbnails cleared from disk and DB")
    }
}
