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
import java.io.File
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
        private val SAFE_FILENAME_REGEX = Regex("[^a-zA-Z0-9._-]")
    }

    private val thumbnailDir: File by lazy {
        File(context.cacheDir, "thumbnails").also { it.mkdirs() }
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

    private suspend fun extractSingleThumbnail(channelId: String, streamUrl: String): Boolean {
        if (streamUrl.isBlank() || !streamUrl.startsWith("http", ignoreCase = true)) {
            return false
        }

        return withContext(dispatcher) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(streamUrl, HashMap<String, String>())
                val frame = retriever.getFrameAtTime(
                    0,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: return@withContext false

                val scaled = Bitmap.createScaledBitmap(
                    frame, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true
                )
                if (scaled !== frame) frame.recycle()

                // Sanitize channelId for safe filename (replace filesystem-invalid chars)
                val safeId = channelId.replace(SAFE_FILENAME_REGEX, "_")
                val file = File(thumbnailDir, "${safeId}.jpg")
                file.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                scaled.recycle()

                channelHealthDao.updateThumbnailPath(channelId, file.absolutePath)
                Log.d(TAG, "Thumbnail saved for $channelId")
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed thumbnail for $channelId: ${e.message}")
                false
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) { }
            }
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
