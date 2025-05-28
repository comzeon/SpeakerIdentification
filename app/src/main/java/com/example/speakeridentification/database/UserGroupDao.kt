package com.example.speakeridentification.database

import androidx.room.*

@Dao
interface UserGroupDao {
    @Query("SELECT * FROM user_groups")
    suspend fun getAll(): List<UserGroup>

    @Insert
    suspend fun insert(group: UserGroup)

    @Delete
    suspend fun delete(group: UserGroup)

    @Update
    suspend fun update(group: UserGroup)
}