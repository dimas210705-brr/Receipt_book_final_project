package com.example.recipebook.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_recipes")
data class CachedRecipeEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val summary: String,
    val cuisinesJson: String,
    val ingredientsJson: String,
    val instructionsJson: String,
    val cachedAtTimestamp: Long = System.currentTimeMillis(),
    val isPinnedOffline: Boolean = false
)

@Entity(tableName = "recipe_history")
data class RecipeHistoryEntity(
    @PrimaryKey val recipeId: Int,
    val title: String,
    val image: String,
    val viewedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_collections")
data class RecipeCollectionEntity(
    @PrimaryKey(autoGenerate = true) val collectionId: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recipe_collection_cross_ref",
    primaryKeys = ["collectionId", "recipeId"],
    indices = [Index("recipeId")]
)
data class RecipeCollectionCrossRef(
    val collectionId: Long,
    val recipeId: Int,
    val recipeTitle: String,
    val recipeImage: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipe_notes")
data class RecipeNoteEntity(
    @PrimaryKey val recipeId: Int,
    val noteText: String = "",
    val rating: Int = 0,
    val isCooked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
