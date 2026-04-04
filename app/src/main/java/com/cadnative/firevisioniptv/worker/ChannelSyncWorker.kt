package com.cadnative.firevisioniptv.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cadnative.firevisioniptv.ChannelManager
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withTimeout

@HiltWorker
class ChannelSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val channelRepository: ChannelRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = withTimeout(SYNC_TIMEOUT_MS) {
                channelRepository.refreshChannels()
            }

            when (result) {
                is com.cadnative.firevisioniptv.data.model.Result.Success -> {
                    // Sync updated channels + EPG to Fire TV TIF database
                    try {
                        ChannelManager.create(applicationContext).syncChannelsToTif()
                    } catch (e: Exception) {
                        Log.e(TAG, "TIF sync failed after channel refresh", e)
                    }
                    Result.success()
                }
                is com.cadnative.firevisioniptv.data.model.Result.Error -> {
                    if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "ChannelSyncWorker"
        const val WORK_NAME = "channel_sync_work"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val SYNC_TIMEOUT_MS = 120_000L // 2 minutes
    }
}
