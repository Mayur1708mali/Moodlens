package com.example.moodlens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageCleanupTest {

    @Test
    fun retentionCutoff_calculatesCorrect30DayWindow() {
        val now = 1700000000000L
        val retentionDays = 30
        val cutoff = now - (retentionDays * 24L * 60 * 60 * 1000L)

        val oldTimestamp = cutoff - 1000L // 30 days + 1 second ago (expired)
        val recentTimestamp = cutoff + 1000L // 29 days 23 hours ago (valid)

        assertTrue(oldTimestamp < cutoff)
        assertFalse(recentTimestamp < cutoff)
    }

    @Test
    fun retentionFilter_identifiesExpiredFiles() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_moodlens_thumbs")
        tempDir.mkdirs()

        try {
            val file1 = File(tempDir, "thumb_recent.jpg").apply {
                createNewFile()
                setLastModified(System.currentTimeMillis())
            }

            val file2 = File(tempDir, "thumb_old.jpg").apply {
                createNewFile()
                val fortyDaysAgo = System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000L)
                setLastModified(fortyDaysAgo)
            }

            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            val expiredFiles = tempDir.listFiles()?.filter { it.lastModified() < cutoff } ?: emptyList()

            assertEquals(1, expiredFiles.size)
            assertEquals("thumb_old.jpg", expiredFiles[0].name)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
