package com.example.merlinmedia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.merlinmedia.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT id FROM favorites")
    fun getAllFavoriteIdsFlow(): Flow<List<String>>

    @Query("SELECT id FROM favorites")
    suspend fun getAllFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()
}
