package com.example.moodlens.data

/**
 * Data class holding the user's check-in streak status.
 */
data class StreakData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCheckInDateMillis: Long = 0L
)
