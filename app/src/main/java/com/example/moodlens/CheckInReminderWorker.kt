package com.example.moodlens

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moodlens.data.SessionRepository
import com.example.moodlens.data.StreakRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that fires once daily.
 * Checks if the user has completed at least one mood check-in today.
 * If not, sends a nudge notification to preserve their streak.
 */
class CheckInReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "CheckInReminderWorker started checking daily status")
        return try {
            val startOfDay = MoodSummaryService.getStartOfDay()
            val endOfDay = MoodSummaryService.getEndOfDay()

            val repository = SessionRepository.getInstance(applicationContext)
            val todayEntries = repository.getSessionsBetween(startOfDay, endOfDay).first()

            if (todayEntries.isEmpty()) {
                Log.i(TAG, "No check-ins found for today. Dispatching reminder notification.")
                val streakRepository = StreakRepository.getInstance(applicationContext)
                val streakData = streakRepository.getStreakData()
                NotificationHelper.showDailyReminderNotification(
                    context = applicationContext,
                    streakDays = streakData.currentStreak
                )
            } else {
                Log.i(TAG, "User already recorded ${todayEntries.size} check-ins today. Skipping notification.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing CheckInReminderWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CheckInReminderWorker"
        const val UNIQUE_WORK_NAME = "moodlens_daily_checkin_reminder"

        /**
         * Schedules a 24-hour periodic daily reminder job at the specified [targetHour]:[targetMinute].
         * Default is 8:00 PM (20:00).
         */
        fun scheduleDailyReminder(
            context: Context,
            targetHour: Int = 20,
            targetMinute: Int = 0
        ) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis

            val periodicWorkRequest = PeriodicWorkRequestBuilder<CheckInReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            Log.i(TAG, "Daily reminder scheduled with initial delay of ${initialDelayMillis / (1000 * 60)} minutes")
        }

        fun cancelDailyReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
