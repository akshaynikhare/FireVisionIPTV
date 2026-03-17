package com.cadnative.firevisioniptv.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cadnative.firevisioniptv.data.source.local.entity.FavoriteCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCategoryDao {

    @Query("SELECT * FROM favorite_categories ORDER BY addedAt DESC")
    fun getAllFavoriteCategories(): Flow<List<FavoriteCategoryEntity>>

    @Query("SELECT categoryName FROM favorite_categories")
    fun getAllFavoriteCategoryNames(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_categories WHERE categoryName = :categoryName)")
    suspend fun isFavorite(categoryName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(entity: FavoriteCategoryEntity)

    @Query("DELETE FROM favorite_categories WHERE categoryName = :categoryName")
    suspend fun removeFavorite(categoryName: String)
}
