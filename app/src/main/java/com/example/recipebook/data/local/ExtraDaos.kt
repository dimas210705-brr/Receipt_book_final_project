package com.example.recipebook.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRecipeDao {
    @Query("SELECT * FROM cached_recipes WHERE id = :id")
    fun observeRecipeById(id: Int): Flow<CachedRecipeEntity?>

    @Query("SELECT * FROM cached_recipes WHERE id = :id")
    suspend fun getRecipeById(id: Int): CachedRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: CachedRecipeEntity)

    @Query("DELETE FROM cached_recipes WHERE isPinnedOffline = 0")
    suspend fun clearUnpinnedCache()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM recipe_history ORDER BY viewedAtTimestamp DESC")
    fun observeHistory(): Flow<List<RecipeHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordView(entry: RecipeHistoryEntity)

    @Query("DELETE FROM recipe_history")
    suspend fun clearHistory()
}

@Dao
interface CollectionsDao {
    @Query("SELECT * FROM user_collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<RecipeCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: RecipeCollectionEntity): Long

    @Query("DELETE FROM user_collections WHERE collectionId = :collectionId")
    suspend fun deleteCollection(collectionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecipeToCollection(crossRef: RecipeCollectionCrossRef)

    @Query("DELETE FROM recipe_collection_cross_ref WHERE collectionId = :collectionId AND recipeId = :recipeId")
    suspend fun removeRecipeFromCollection(collectionId: Long, recipeId: Int)

    @Query("SELECT * FROM recipe_collection_cross_ref WHERE collectionId = :collectionId")
    fun observeRecipesInCollection(collectionId: Long): Flow<List<RecipeCollectionCrossRef>>

    @Query("SELECT * FROM recipe_notes WHERE recipeId = :recipeId")
    fun observeNoteForRecipe(recipeId: Int): Flow<RecipeNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: RecipeNoteEntity)
}
