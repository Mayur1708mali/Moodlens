package com.example.moodlens.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SessionEntry] persistence.
 */
@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SessionEntry): Long

    @Update
    suspend fun update(entry: SessionEntry)

    @Delete
    suspend fun delete(entry: SessionEntry)

    @Query("DELETE FROM session_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM session_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM session_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntry?

    @Query("SELECT * FROM session_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SessionEntry>>

    @Query("SELECT * FROM session_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getEntriesBetween(startTime: Long, endTime: Long): Flow<List<SessionEntry>>

    @Query("SELECT COUNT(*) FROM session_entries")
    fun getCount(): Flow<Int>
}
