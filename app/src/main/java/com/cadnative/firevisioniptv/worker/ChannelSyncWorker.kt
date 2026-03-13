package com.cadnative.firevisioniptv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cadnative.firevisioniptv.domain.repository.ChannelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker for periodic channel synchronization.
 */
@HiltWorker
class ChannelSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val channelRepository: ChannelRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Refresh channels from server
            channelRepository.refreshChannels().first()
            
            Result.success()
        } catch (e: Exception) {
            // Retry with exponential backoff
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "channel_sync_work"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
