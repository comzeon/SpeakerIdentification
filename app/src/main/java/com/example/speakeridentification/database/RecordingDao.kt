package com.example.speakeridentification.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording)

    @Query("SELECT * FROM Recording WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getRecordingsForUser(userId: Long): List<Recording>

    @Query("SELECT * FROM Recording")
    suspend fun getAll(): List<Recording>

    @Delete
    suspend fun delete(recording: Recording)

    @Update
    suspend fun update(recording: Recording)
}
