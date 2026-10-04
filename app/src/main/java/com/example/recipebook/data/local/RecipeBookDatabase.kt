package com.example.recipebook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavouriteMealEntity::class,
        CachedRecipeEntity::class,
        RecipeHistoryEntity::class,
        RecipeCollectionEntity::class,
        RecipeCollectionCrossRef::class,
        RecipeNoteEntity::class
    ],
    version = RecipeBookDatabase.VERSION,
    exportSchema = false
)
abstract class RecipeBookDatabase : RoomDatabase() {

    abstract fun favouriteDao(): FavouriteDao
    abstract fun offlineRecipeDao(): OfflineRecipeDao
    abstract fun historyDao(): HistoryDao
    abstract fun collectionsDao(): CollectionsDao

    companion object {
        const val VERSION = 2
        const val NAME = "recipebook_v2.db"
    }
}
