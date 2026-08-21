package com.example.moodlens

import com.example.moodlens.data.StreakData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakServiceTest {

    @Test
    fun computeUpdatedStreak_firstCheckIn_setsStreakToOne() {
        val initial = StreakData(currentStreak = 0, bestStreak = 0, lastCheckInDateMillis = 0L)
        val now = 1700000000000L

        val updated = StreakService.computeUpdatedStreak(initial, now)

        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.bestStreak)
        assertEquals(now, updated.lastCheckInDateMillis)
    }

    @Test
    fun computeUpdatedStreak_sameDayCheckIn_doesNotIncrementStreak() {
        val baseTime = 1700000000000L
        val initial = StreakData(currentStreak = 3, bestStreak = 5, lastCheckInDateMillis = baseTime)
        // 2 hours later on the same day
        val sameDayLater = baseTime + (2 * 60 * 60 * 1000L)

        val updated = StreakService.computeUpdatedStreak(initial, sameDayLater)

        assertEquals(3, updated.currentStreak)
        assertEquals(5, updated.bestStreak)
        assertEquals(sameDayLater, updated.lastCheckInDateMillis)
    }

    @Test
    fun computeUpdatedStreak_consecutiveDay_incrementsStreakAndUpdatesBest() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 20, 14, 0, 0)
        }
        val day1 = cal.timeInMillis

        val initial = StreakData(currentStreak = 2, bestStreak = 2, lastCheckInDateMillis = day1)

        cal.set(2026, Calendar.AUGUST, 21, 10, 0, 0)
        val day2 = cal.timeInMillis

        val updated = StreakService.computeUpdatedStreak(initial, day2)

        assertEquals(3, updated.currentStreak)
        assertEquals(3, updated.bestStreak)
        assertEquals(day2, updated.lastCheckInDateMillis)
    }

    @Test
    fun computeUpdatedStreak_brokenStreak_resetsCurrentToOneAndPreservesBest() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 14, 0, 0)
        }
        val day1 = cal.timeInMillis

        val initial = StreakData(currentStreak = 5, bestStreak = 10, lastCheckInDateMillis = day1)

        // 3 days later
        cal.set(2026, Calendar.AUGUST, 18, 10, 0, 0)
        val day4 = cal.timeInMillis

        val updated = StreakService.computeUpdatedStreak(initial, day4)

        assertEquals(1, updated.currentStreak)
        assertEquals(10, updated.bestStreak) // Preserves historical best
        assertEquals(day4, updated.lastCheckInDateMillis)
    }

    @Test
    fun isSameDay_and_isConsecutiveDay_calendarAccuracy() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 23, 59, 0)
        }
        val endOfJan = cal.timeInMillis

        cal.set(2026, Calendar.FEBRUARY, 1, 0, 1, 0)
        val startOfFeb = cal.timeInMillis

        assertFalse(StreakService.isSameDay(endOfJan, startOfFeb))
        assertTrue(StreakService.isConsecutiveDay(endOfJan, startOfFeb))
    }
}
