package com.example.appfall.data.repositories

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appfall.data.daoModels.UserDaoModel

import com.example.appfall.data.repositories.dataStorage.UserDao

@Database(entities = [UserDaoModel::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "Fall_db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(3, 1) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table for contacts
                database.execSQL("DROP TABLE IF EXISTS users")
                database.execSQL("DROP TABLE IF EXISTS contacts")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "name TEXT NOT NULL DEFAULT 'undefined', " +
                            "phone TEXT NOT NULL DEFAULT 'undefined', " +
                            "token TEXT NOT NULL DEFAULT 'undefined', " +
                            "PRIMARY KEY(phone)" +
                            ")"
                )
            }
        }
    }
}