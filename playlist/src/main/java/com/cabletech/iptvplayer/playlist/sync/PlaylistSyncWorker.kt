package com.cabletech.iptvplayer.playlist.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.playlist.ChannelSourceRepository
import com.cabletech.iptvplayer.playlist.PlaylistFetcher
import com.cabletech.iptvplayer.playlist.db.PlaylistDatabase
import java.util.concurrent.TimeUnit

/**
 * Background [CoroutineWorker] that refreshes all enabled M3U sources.
 *
 * Scheduled via [WorkManager] to run every 6 hours when a network connection
 * is available.
 */
class PlaylistSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = PlaylistDatabase.getInstance(applicationContext)
        val repository = ChannelSourceRepository(
            sourceDao = db.channelSourceDao(),
            channelDao = db.channelDao(),
            fetcher = PlaylistFetcher(OkHttpClientFactory.create()),
        )
        return runCatching { repository.refreshAllEnabled() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    companion object {
        private const val WORK_NAME = "playlist_sync"
        private const val REPEAT_HOURS = 6L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
                REPEAT_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
