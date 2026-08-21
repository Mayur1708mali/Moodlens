package com.example.moodlens

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodlens.data.SessionEntry
import com.example.moodlens.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Mood Journal entries, selection, and deletion.
 */
class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SessionRepository.getInstance(application)
    private val streakRepository = com.example.moodlens.data.StreakRepository.getInstance(application)

    val streakData: StateFlow<com.example.moodlens.data.StreakData> = streakRepository.streakData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.moodlens.data.StreakData()
        )

    val entries: StateFlow<List<SessionEntry>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = repository.sessionCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _selectedEntry = MutableStateFlow<SessionEntry?>(null)
    val selectedEntry: StateFlow<SessionEntry?> = _selectedEntry.asStateFlow()

    fun selectEntry(entry: SessionEntry?) {
        _selectedEntry.value = entry
    }

    fun deleteEntry(entry: SessionEntry) {
        viewModelScope.launch {
            repository.deleteSession(entry)
            if (_selectedEntry.value?.id == entry.id) {
                _selectedEntry.value = null
            }
        }
    }

    suspend fun loadThumbnail(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return repository.loadThumbnail(path)
    }
}
