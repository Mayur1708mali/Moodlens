package com.example.moodlens

import com.example.moodlens.data.DailySummary
import com.example.moodlens.data.MoodDip
import com.example.moodlens.data.SessionEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Service that aggregates mood entries for a day, calculates dominant emotions,
 * and detects emotional dips (clusters of negative emotions within a time window).
 */
object MoodSummaryService {

    /** Default time window for dip clustering: 2 hours in milliseconds. */
    const val DIP_WINDOW_MILLIS = 2 * 60 * 60 * 1000L

    /** Minimum count of negative check-ins within the time window to constitute a dip. */
    const val MIN_NEGATIVE_COUNT_FOR_DIP = 2

    val NEGATIVE_EMOTIONS = setOf("sad", "angry", "fear", "disgust", "contempt")
    val POSITIVE_EMOTIONS = setOf("happy", "surprise")
    val NEUTRAL_EMOTIONS = setOf("neutral")

    /**
     * Checks if an emotion label is classified as negative.
     */
    fun isNegativeEmotion(label: String): Boolean {
        return NEGATIVE_EMOTIONS.contains(label.lowercase(Locale.ROOT))
    }

    /**
     * Aggregates session entries for a specific day into a [DailySummary].
     *
     * @param entries List of [SessionEntry] recorded during the day.
     * @param dayStartMillis Epoch timestamp for the start of the day (00:00:00).
     */
    fun computeDailySummary(entries: List<SessionEntry>, dayStartMillis: Long): DailySummary {
        if (entries.isEmpty()) {
            return DailySummary(
                dayStartMillis = dayStartMillis,
                totalCheckIns = 0,
                dominantEmotion = null,
                dominantEmotionPercentage = 0f,
                emotionCounts = emptyMap(),
                averageConfidence = 0f,
                moodDips = emptyList(),
                entries = emptyList()
            )
        }

        // Sort entries chronologically ascending for pipeline analysis
        val sortedEntries = entries.sortedBy { it.timestamp }

        // Frequency breakdown by emotion label
        val emotionCounts = mutableMapOf<String, Int>()
        var totalConfidence = 0f

        for (entry in sortedEntries) {
            val normalizedLabel = entry.emotionLabel.lowercase(Locale.ROOT)
            emotionCounts[normalizedLabel] = (emotionCounts[normalizedLabel] ?: 0) + 1
            totalConfidence += entry.confidence
        }

        val totalCount = sortedEntries.size
        val avgConfidence = totalConfidence / totalCount

        // Dominant Emotion calculation
        val dominantEntry = emotionCounts.maxByOrNull { it.value }
        val dominantEmotion = dominantEntry?.key
        val dominantPercentage = dominantEntry?.let { it.value.toFloat() / totalCount } ?: 0f

        // Dip Detection
        val moodDips = detectMoodDips(sortedEntries)

        return DailySummary(
            dayStartMillis = dayStartMillis,
            totalCheckIns = totalCount,
            dominantEmotion = dominantEmotion,
            dominantEmotionPercentage = dominantPercentage,
            emotionCounts = emotionCounts,
            averageConfidence = avgConfidence,
            moodDips = moodDips,
            entries = sortedEntries
        )
    }

    /**
     * Detects clusters of negative emotions occurring within [DIP_WINDOW_MILLIS].
     */
    fun detectMoodDips(sortedEntries: List<SessionEntry>): List<MoodDip> {
        val dips = mutableListOf<MoodDip>()
        val negativeEntries = sortedEntries.filter { isNegativeEmotion(it.emotionLabel) }

        if (negativeEntries.size < MIN_NEGATIVE_COUNT_FOR_DIP) {
            return dips
        }

        var clusterStart = 0
        while (clusterStart < negativeEntries.size) {
            var clusterEnd = clusterStart
            val firstInCluster = negativeEntries[clusterStart]

            // Expand window up to DIP_WINDOW_MILLIS from first entry
            while (clusterEnd + 1 < negativeEntries.size &&
                negativeEntries[clusterEnd + 1].timestamp - firstInCluster.timestamp <= DIP_WINDOW_MILLIS
            ) {
                clusterEnd++
            }

            val clusterSize = clusterEnd - clusterStart + 1
            if (clusterSize >= MIN_NEGATIVE_COUNT_FOR_DIP) {
                val clusterList = negativeEntries.subList(clusterStart, clusterEnd + 1)
                val startTime = clusterList.first().timestamp
                val endTime = clusterList.last().timestamp

                // Find the dominant negative emotion in this cluster
                val dominantNeg = clusterList
                    .groupBy { it.emotionLabel.lowercase(Locale.ROOT) }
                    .maxByOrNull { it.value.size }?.key ?: "negative"

                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeRangeStr = if (startTime == endTime) {
                    timeFormat.format(Date(startTime))
                } else {
                    "${timeFormat.format(Date(startTime))} – ${timeFormat.format(Date(endTime))}"
                }

                val description = "$clusterSize ${dominantNeg.replaceFirstChar { it.uppercase() }} check-ins recorded between $timeRangeStr"

                dips.add(
                    MoodDip(
                        startTimeMillis = startTime,
                        endTimeMillis = endTime,
                        negativeCount = clusterSize,
                        dominantNegativeEmotion = dominantNeg,
                        description = description
                    )
                )

                // Advance past this cluster
                clusterStart = clusterEnd + 1
            } else {
                clusterStart++
            }
        }

        return dips
    }

    /**
     * Returns the epoch timestamp in millis for the start of the day (00:00:00.000).
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Returns the epoch timestamp in millis for the end of the day (23:59:59.999).
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
