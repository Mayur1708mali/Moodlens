package com.example.moodlens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.streakDataStore: DataStore<Preferences> by preferencesDataStore(name = "streak_preferences")

/**
 * DataStore-backed repository storing streak information (current streak, best streak, last check-in date).
 */
class StreakRepository private constructor(private val context: Context) {

    companion object {
        private val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        private val KEY_BEST_STREAK = intPreferencesKey("best_streak")
        private val KEY_LAST_CHECK_IN_DATE = longPreferencesKey("last_check_in_date")

        @Volatile
        private var INSTANCE: StreakRepository? = null

        fun getInstance(context: Context): StreakRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StreakRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val streakData: Flow<StreakData> = context.streakDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val currentStreak = preferences[KEY_CURRENT_STREAK] ?: 0
            val bestStreak = preferences[KEY_BEST_STREAK] ?: 0
            val lastCheckInDate = preferences[KEY_LAST_CHECK_IN_DATE] ?: 0L
            StreakData(
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                lastCheckInDateMillis = lastCheckInDate
            )
        }

    suspend fun getStreakData(): StreakData {
        return streakData.first()
    }

    suspend fun updateStreak(newStreakData: StreakData) {
        context.streakDataStore.edit { preferences ->
            preferences[KEY_CURRENT_STREAK] = newStreakData.currentStreak
            preferences[KEY_BEST_STREAK] = newStreakData.bestStreak
            preferences[KEY_LAST_CHECK_IN_DATE] = newStreakData.lastCheckInDateMillis
        }
    }
}
