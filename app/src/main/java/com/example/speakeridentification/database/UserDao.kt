package com.example.speakeridentification.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM User WHERE groupId = :groupId")
    suspend fun getUsersByGroupId(groupId: Long): List<User>

    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getUserById(id: Long): User

    @Delete
    suspend fun delete(user: User)

    @Update
    suspend fun update(user: User)
}
