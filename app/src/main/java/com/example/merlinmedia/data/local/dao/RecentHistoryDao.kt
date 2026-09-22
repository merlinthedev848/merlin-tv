package com.example.merlinmedia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.merlinmedia.data.local.entity.RecentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentHistoryDao {

    @Query("SELECT * FROM recent_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getRecentHistoryFlow(limit: Int = 40): Flow<List<RecentItemEntity>>

    @Query("SELECT * FROM recent_history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 40): List<RecentItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentItem(item: RecentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentItems(items: List<RecentItemEntity>)

    @Query("DELETE FROM recent_history WHERE url = :url")
    suspend fun deleteRecentItemByUrl(url: String)

    @Query("DELETE FROM recent_history WHERE url NOT IN (SELECT url FROM recent_history ORDER BY watchedAt DESC LIMIT :limit)")
    suspend fun trimRecentHistory(limit: Int = 40)

    @Query("DELETE FROM recent_history")
    suspend fun clearAllRecentHistory()
}
