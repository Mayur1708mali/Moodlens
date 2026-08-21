package com.example.moodlens.data

/**
 * Represents an identified cluster of negative emotions within a short time window.
 */
data class MoodDip(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val negativeCount: Int,
    val dominantNegativeEmotion: String,
    val description: String
)

/**
 * Aggregated summary of emotional check-ins for a specific day.
 */
data class DailySummary(
    val dayStartMillis: Long,
    val totalCheckIns: Int,
    val dominantEmotion: String?,
    val dominantEmotionPercentage: Float,
    val emotionCounts: Map<String, Int>,
    val averageConfidence: Float,
    val moodDips: List<MoodDip>,
    val entries: List<SessionEntry>
) {
    val isDipDetected: Boolean
        get() = moodDips.isNotEmpty()
}
