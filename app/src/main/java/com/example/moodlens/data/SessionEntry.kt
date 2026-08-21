package com.example.moodlens.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a single mood detection check-in / session.
 *
 * @property id Unique auto-generated identifier.
 * @property timestamp Unix epoch timestamp in milliseconds when the session occurred.
 * @property emotionLabel Top classified emotion label (e.g. "happy", "neutral").
 * @property confidence Confidence score in [0.0, 1.0] for the detected emotion.
 * @property thumbnailPath Path to the saved face thumbnail image in internal storage.
 * @property notes Optional user notes or reflections associated with this check-in.
 */
@Entity(tableName = "session_entries")
data class SessionEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val emotionLabel: String,
    val confidence: Float,
    val thumbnailPath: String? = null,
    val notes: String? = null
)
