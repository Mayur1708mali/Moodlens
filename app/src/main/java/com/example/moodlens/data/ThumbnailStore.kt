package com.example.moodlens.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages saving, loading, and deleting face thumbnail images in internal app storage.
 */
class ThumbnailStore(private val context: Context) {

    private val thumbnailDir: File by lazy {
        File(context.filesDir, THUMBNAILS_FOLDER).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Saves a cropped face Bitmap as a JPEG file in internal storage.
     * Runs on IO dispatcher.
     *
     * @param bitmap The cropped face image to save.
     * @param quality JPEG compression quality (0–100), defaults to 85.
     * @return The absolute path of the saved file, or null if writing failed.
     */
    suspend fun saveThumbnail(bitmap: Bitmap, quality: Int = 85): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileName = "thumb_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val file = File(thumbnailDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            Log.i(TAG, "Thumbnail saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thumbnail", e)
            null
        }
    }

    /**
     * Loads a thumbnail Bitmap from the given file path.
     * Runs on IO dispatcher.
     */
    suspend fun loadThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                Log.w(TAG, "Thumbnail file does not exist: $path")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load thumbnail from $path", e)
            null
        }
    }

    /**
     * Deletes a thumbnail file at the specified path.
     */
    suspend fun deleteThumbnail(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            if (file.exists()) {
                file.delete().also { deleted ->
                    if (deleted) Log.i(TAG, "Thumbnail deleted: $path")
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete thumbnail at $path", e)
            false
        }
    }

    /**
     * Returns all thumbnail files currently in storage.
     */
    fun getAllThumbnailFiles(): List<File> {
        return thumbnailDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Deletes thumbnail files older than [retentionDays] days.
     * @return Count of deleted files.
     */
    suspend fun cleanOldThumbnails(retentionDays: Int = 30): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000L)
        var deletedCount = 0
        thumbnailDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        if (deletedCount > 0) {
            Log.i(TAG, "Cleaned up $deletedCount old thumbnails older than $retentionDays days")
        }
        deletedCount
    }

    /**
     * Clears all saved thumbnail files.
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            thumbnailDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear thumbnails", e)
            false
        }
    }

    companion object {
        private const val TAG = "ThumbnailStore"
        private const val THUMBNAILS_FOLDER = "thumbnails"
    }
}
