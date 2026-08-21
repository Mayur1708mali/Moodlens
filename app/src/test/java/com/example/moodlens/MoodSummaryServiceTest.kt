package com.example.moodlens

import com.example.moodlens.data.SessionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodSummaryServiceTest {

    @Test
    fun computeDailySummary_emptyList_returnsEmptySummary() {
        val summary = MoodSummaryService.computeDailySummary(emptyList(), 1000L)
        assertEquals(0, summary.totalCheckIns)
        assertNull(summary.dominantEmotion)
        assertEquals(0f, summary.dominantEmotionPercentage, 0.001f)
        assertFalse(summary.isDipDetected)
        assertTrue(summary.moodDips.isEmpty())
    }

    @Test
    fun computeDailySummary_singleEmotion_calculatesDominantCorrectly() {
        val entry = SessionEntry(id = 1, timestamp = 1000L, emotionLabel = "happy", confidence = 0.9f)
        val summary = MoodSummaryService.computeDailySummary(listOf(entry), 1000L)

        assertEquals(1, summary.totalCheckIns)
        assertEquals("happy", summary.dominantEmotion)
        assertEquals(1.0f, summary.dominantEmotionPercentage, 0.001f)
        assertEquals(0.9f, summary.averageConfidence, 0.001f)
        assertFalse(summary.isDipDetected)
    }

    @Test
    fun computeDailySummary_multipleEmotions_selectsMostFrequentDominant() {
        val baseTime = 1700000000000L
        val entries = listOf(
            SessionEntry(id = 1, timestamp = baseTime, emotionLabel = "happy", confidence = 0.8f),
            SessionEntry(id = 2, timestamp = baseTime + 1000, emotionLabel = "happy", confidence = 0.9f),
            SessionEntry(id = 3, timestamp = baseTime + 2000, emotionLabel = "neutral", confidence = 0.7f),
            SessionEntry(id = 4, timestamp = baseTime + 3000, emotionLabel = "sad", confidence = 0.6f)
        )

        val summary = MoodSummaryService.computeDailySummary(entries, baseTime)

        assertEquals(4, summary.totalCheckIns)
        assertEquals("happy", summary.dominantEmotion)
        assertEquals(0.5f, summary.dominantEmotionPercentage, 0.001f)
        assertEquals(2, summary.emotionCounts["happy"])
        assertEquals(1, summary.emotionCounts["neutral"])
        assertEquals(1, summary.emotionCounts["sad"])
    }

    @Test
    fun detectMoodDips_clusterOfNegativeEmotions_detectsDip() {
        val baseTime = 1700000000000L
        // Two negative emotions within 30 minutes (< 2 hours window)
        val entries = listOf(
            SessionEntry(id = 1, timestamp = baseTime, emotionLabel = "happy", confidence = 0.9f),
            SessionEntry(id = 2, timestamp = baseTime + (10 * 60 * 1000), emotionLabel = "sad", confidence = 0.8f),
            SessionEntry(id = 3, timestamp = baseTime + (30 * 60 * 1000), emotionLabel = "angry", confidence = 0.85f),
            SessionEntry(id = 4, timestamp = baseTime + (4 * 60 * 60 * 1000), emotionLabel = "happy", confidence = 0.95f)
        )

        val dips = MoodSummaryService.detectMoodDips(entries)

        assertEquals(1, dips.size)
        val dip = dips[0]
        assertEquals(2, dip.negativeCount)
        assertTrue(dip.dominantNegativeEmotion in listOf("sad", "angry"))
        assertTrue(dip.description.contains("check-ins recorded"))
    }

    @Test
    fun detectMoodDips_spreadOutNegativeEmotions_noDipDetected() {
        val baseTime = 1700000000000L
        // Two negative emotions 5 hours apart (> 2 hours window)
        val entries = listOf(
            SessionEntry(id = 1, timestamp = baseTime, emotionLabel = "sad", confidence = 0.8f),
            SessionEntry(id = 2, timestamp = baseTime + (5 * 60 * 60 * 1000), emotionLabel = "sad", confidence = 0.8f)
        )

        val dips = MoodSummaryService.detectMoodDips(entries)
        assertTrue(dips.isEmpty())
    }
}
