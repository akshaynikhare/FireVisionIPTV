package com.cabletech.iptvplayer.epg.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cabletech.iptvplayer.core.network.OkHttpClientFactory
import com.cabletech.iptvplayer.epg.EpgRepository
import com.cabletech.iptvplayer.epg.db.EpgDatabase
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that refreshes all enabled XMLTV EPG sources.
 *
 * Runs every 12 hours when a network connection is available.
 */
class EpgSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = EpgRepository(
            dao = EpgDatabase.getInstance(applicationContext).epgDao(),
            client = OkHttpClientFactory.create(),
        )
        return runCatching { repository.refreshAll() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val WORK_NAME = "epg_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
