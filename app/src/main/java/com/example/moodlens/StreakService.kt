package com.example.moodlens

import android.content.Context
import com.example.moodlens.data.StreakData
import com.example.moodlens.data.StreakRepository
import java.util.Calendar
import kotlin.math.max

/**
 * Service providing streak calculation, consecutive day evaluation, and check-in updates.
 */
object StreakService {

    /**
     * Evaluates the new streak state given the previous state and new check-in timestamp.
     */
    fun computeUpdatedStreak(
        currentStreakData: StreakData,
        checkInTimestamp: Long = System.currentTimeMillis()
    ): StreakData {
        if (currentStreakData.lastCheckInDateMillis == 0L) {
            // First ever check-in
            return StreakData(
                currentStreak = 1,
                bestStreak = max(1, currentStreakData.bestStreak),
                lastCheckInDateMillis = checkInTimestamp
            )
        }

        if (isSameDay(currentStreakData.lastCheckInDateMillis, checkInTimestamp)) {
            // Multiple check-ins on the same day: streak doesn't increment again
            return currentStreakData.copy(lastCheckInDateMillis = checkInTimestamp)
        }

        if (isConsecutiveDay(currentStreakData.lastCheckInDateMillis, checkInTimestamp)) {
            // Checked in yesterday: streak increments
            val newStreak = currentStreakData.currentStreak + 1
            return StreakData(
                currentStreak = newStreak,
                bestStreak = max(currentStreakData.bestStreak, newStreak),
                lastCheckInDateMillis = checkInTimestamp
            )
        }

        // Broken streak (> 1 day gap)
        return StreakData(
            currentStreak = 1,
            bestStreak = max(currentStreakData.bestStreak, 1),
            lastCheckInDateMillis = checkInTimestamp
        )
    }

    /**
     * Checks if two epoch timestamps fall on the exact same calendar day.
     */
    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if time2 is exactly the next calendar day after time1.
     */
    fun isConsecutiveDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Add 1 day to cal1
        cal1.add(Calendar.DAY_OF_YEAR, 1)
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Records a check-in, updates DataStore persistence, and returns the new [StreakData].
     */
    suspend fun recordCheckIn(
        context: Context,
        checkInTimestamp: Long = System.currentTimeMillis()
    ): StreakData {
        val repository = StreakRepository.getInstance(context)
        val currentData = repository.getStreakData()
        val updatedData = computeUpdatedStreak(currentData, checkInTimestamp)
        repository.updateStreak(updatedData)
        return updatedData
    }
}
