package com.example.merlinmedia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.merlinmedia.data.local.dao.FavoritesDao
import com.example.merlinmedia.data.local.dao.RecentHistoryDao
import com.example.merlinmedia.data.local.entity.FavoriteEntity
import com.example.merlinmedia.data.local.entity.RecentItemEntity

@Database(
    entities = [
        FavoriteEntity::class,
        RecentItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao
    abstract fun recentHistoryDao(): RecentHistoryDao

    companion object {
        private const val DATABASE_NAME = "merlin_tv.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
