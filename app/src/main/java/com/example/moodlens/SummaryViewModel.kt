package com.example.moodlens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodlens.data.DailySummary
import com.example.moodlens.data.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel managing the Daily Summary aggregation, date selection, and dip analytics.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SessionRepository.getInstance(application)

    private val _selectedDateMillis = MutableStateFlow(MoodSummaryService.getStartOfDay())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    /**
     * Flow of [DailySummary] for the currently selected date.
     */
    val dailySummary: StateFlow<DailySummary> = _selectedDateMillis
        .flatMapLatest { startOfDay ->
            val endOfDay = MoodSummaryService.getEndOfDay(startOfDay)
            repository.getSessionsBetween(startOfDay, endOfDay).map { entries ->
                MoodSummaryService.computeDailySummary(entries, startOfDay)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoodSummaryService.computeDailySummary(emptyList(), MoodSummaryService.getStartOfDay())
        )

    fun selectDate(timestampMillis: Long) {
        _selectedDateMillis.value = MoodSummaryService.getStartOfDay(timestampMillis)
    }

    fun selectPreviousDay() {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        _selectedDateMillis.value = MoodSummaryService.getStartOfDay(_selectedDateMillis.value - oneDayMillis)
    }

    fun selectNextDay() {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val nextDay = MoodSummaryService.getStartOfDay(_selectedDateMillis.value + oneDayMillis)
        val today = MoodSummaryService.getStartOfDay()
        if (nextDay <= today) {
            _selectedDateMillis.value = nextDay
        }
    }

    fun selectToday() {
        _selectedDateMillis.value = MoodSummaryService.getStartOfDay()
    }
}
