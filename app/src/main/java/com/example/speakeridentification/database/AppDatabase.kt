package com.example.speakeridentification.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.util.Log

@Database(entities = [UserGroup::class, User::class, Recording::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userGroupDao(): UserGroupDao
    abstract fun userDao(): UserDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            Log.d("DB_PATH", context.getDatabasePath("group_database").absolutePath)
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "group_database"
                ).build().also { instance = it }
            }
        }
    }
}
