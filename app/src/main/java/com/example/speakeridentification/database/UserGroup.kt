package com.example.speakeridentification.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_groups")
data class UserGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
