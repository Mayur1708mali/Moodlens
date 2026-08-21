package com.example.moodlens.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * Repository coordinating Room database operations with [ThumbnailStore] internal file persistence.
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val thumbnailStore: ThumbnailStore
) {

    /**
     * Flow of all session entries ordered by timestamp descending.
     */
    val allSessions: Flow<List<SessionEntry>> = sessionDao.getAll()

    /**
     * Flow of total session count.
     */
    val sessionCount: Flow<Int> = sessionDao.getCount()

    /**
     * Saves a new mood session record, saving the face thumbnail to disk first if provided.
     *
     * @param emotionLabel Top classified emotion label.
     * @param confidence Confidence score in [0.0, 1.0].
     * @param faceBitmap Optional cropped face image to persist.
     * @param notes Optional reflection text.
     * @return The auto-generated database ID of the inserted record.
     */
    suspend fun saveSession(
        emotionLabel: String,
        confidence: Float,
        faceBitmap: Bitmap? = null,
        notes: String? = null
    ): Long {
        val thumbnailPath = faceBitmap?.let { thumbnailStore.saveThumbnail(it) }
        val entry = SessionEntry(
            timestamp = System.currentTimeMillis(),
            emotionLabel = emotionLabel,
            confidence = confidence,
            thumbnailPath = thumbnailPath,
            notes = notes
        )
        return sessionDao.insert(entry)
    }

    /**
     * Retrieves session entries recorded within the given timestamp interval.
     */
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<SessionEntry>> {
        return sessionDao.getEntriesBetween(startTime, endTime)
    }

    /**
     * Retrieves a single session by its database ID.
     */
    suspend fun getSessionById(id: Long): SessionEntry? {
        return sessionDao.getById(id)
    }

    /**
     * Deletes a session entry and removes its associated thumbnail file from disk.
     */
    suspend fun deleteSession(entry: SessionEntry) {
        entry.thumbnailPath?.let { path ->
            thumbnailStore.deleteThumbnail(path)
        }
        sessionDao.delete(entry)
    }

    /**
     * Deletes a session entry by ID, cleaning up its thumbnail file.
     */
    suspend fun deleteSessionById(id: Long) {
        val entry = sessionDao.getById(id)
        if (entry != null) {
            deleteSession(entry)
        }
    }

    /**
     * Loads a thumbnail bitmap from internal storage.
     */
    suspend fun loadThumbnail(path: String): Bitmap? {
        return thumbnailStore.loadThumbnail(path)
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionRepository? = null

        fun getInstance(context: Context): SessionRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val database = AppDatabase.getDatabase(appContext)
                val thumbnailStore = ThumbnailStore(appContext)
                val repository = SessionRepository(database.sessionDao(), thumbnailStore)
                INSTANCE = repository
                repository
            }
        }
    }
}
