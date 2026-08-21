package com.example.moodlens

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moodlens.data.ThumbnailStore
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that runs weekly to enforce the 30-day thumbnail retention policy,
 * freeing up internal disk space from aged face captures.
 */
class StorageCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "StorageCleanupWorker started")
        return try {
            val thumbnailStore = ThumbnailStore(applicationContext)
            val deletedCount = thumbnailStore.cleanOldThumbnails(retentionDays = 30)
            Log.i(TAG, "StorageCleanupWorker completed: removed $deletedCount expired thumbnails")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing StorageCleanupWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "StorageCleanupWorker"
        const val UNIQUE_WORK_NAME = "moodlens_storage_cleanup"

        /**
         * Enqueues a weekly recurring cleanup job when device is idle and not low on battery.
         */
        fun scheduleWeeklyCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<StorageCleanupWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            Log.i(TAG, "Weekly storage cleanup worker scheduled")
        }
    }
}
