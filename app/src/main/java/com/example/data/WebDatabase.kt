package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WebProject::class, ProjectBackup::class], version = 1, exportSchema = false)
abstract class WebDatabase : RoomDatabase() {
    abstract fun webDao(): WebDao

    companion object {
        @Volatile
        private var INSTANCE: WebDatabase? = null

        fun getDatabase(context: Context): WebDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WebDatabase::class.java,
                    "webai_copilot_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
