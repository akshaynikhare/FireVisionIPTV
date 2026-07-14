package com.cadnative.firevisioniptv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cadnative.firevisioniptv.domain.repository.EpgRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withTimeout

/**
 * Periodically refreshes the EPG cache in the background so the guide stays current
 * without depending on the app being reopened. Refresh never wipes last-good data,
 * so a failed run is harmless.
 */
@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgRepository: EpgRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withTimeout(SYNC_TIMEOUT_MS) {
                epgRepository.refreshNow()
            }
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "epg_sync_work"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val SYNC_TIMEOUT_MS = 120_000L // 2 minutes
    }
}
