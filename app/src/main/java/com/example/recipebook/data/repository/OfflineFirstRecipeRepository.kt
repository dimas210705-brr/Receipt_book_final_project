package com.example.recipebook.data.repository

import com.example.recipebook.api.MealApi
import com.example.recipebook.api.toRecipe
import com.example.recipebook.data.local.CachedRecipeEntity
import com.example.recipebook.data.local.OfflineRecipeDao
import com.example.recipebook.data.preferences.UserSettingsRepository
import com.example.recipebook.models.Ingredient
import com.example.recipebook.models.Instruction
import com.example.recipebook.models.Recipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstRecipeRepository @Inject constructor(
    private val api: MealApi,
    private val offlineDao: OfflineRecipeDao,
    private val userSettingsRepository: UserSettingsRepository,
    private val gson: Gson
) {
    fun observeRecipe(mealId: Int): Flow<Recipe?> {
        return offlineDao.observeRecipeById(mealId).map { entity ->
            entity?.toDomainRecipe(gson)
        }
    }

    suspend fun getCachedRecipe(mealId: Int): Recipe? {
        return offlineDao.getRecipeById(mealId)?.toDomainRecipe(gson)
    }

    suspend fun refreshRecipe(mealId: Int, force: Boolean = false): Result<Recipe> {
        val cached = offlineDao.getRecipeById(mealId)
        val settings = userSettingsRepository.settingsFlow.first()
        val ttlMillis = settings.cacheTtlHours * 3600 * 1000L
        val isExpired = cached == null || (System.currentTimeMillis() - cached.cachedAtTimestamp > ttlMillis)

        if (!force && !isExpired && cached != null) {
            return Result.success(cached.toDomainRecipe(gson))
        }

        return try {
            val freshRecipe = fetchFromNetwork(mealId)
            offlineDao.insertRecipe(freshRecipe.toCachedEntity(gson, isPinned = cached?.isPinnedOffline ?: false))
            Result.success(freshRecipe)
        } catch (e: Exception) {
            if (cached != null) {
                Result.success(cached.toDomainRecipe(gson))
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchFromNetwork(mealId: Int): Recipe = coroutineScope {
        val infoDeferred = async { api.getRecipeInformation(mealId) }
        val instructionsDeferred = async { api.getAnalyzedInstructions(mealId) }
        val info = infoDeferred.await()
        val instructions = instructionsDeferred.await()
        info.toRecipe(instructions)
    }

    suspend fun clearCache() {
        offlineDao.clearUnpinnedCache()
    }
}

fun CachedRecipeEntity.toDomainRecipe(gson: Gson): Recipe {
    val cuisinesType = object : TypeToken<List<String>>() {}.type
    val ingredientsType = object : TypeToken<List<Ingredient>>() {}.type
    val instructionsType = object : TypeToken<List<Instruction>>() {}.type
    return Recipe(
        id = id,
        title = title,
        image = image,
        readyInMinutes = readyInMinutes,
        summary = summary,
        cuisines = gson.fromJson(cuisinesJson, cuisinesType) ?: emptyList(),
        ingredients = gson.fromJson(ingredientsJson, ingredientsType) ?: emptyList(),
        instructions = gson.fromJson(instructionsJson, instructionsType) ?: emptyList()
    )
}

fun Recipe.toCachedEntity(gson: Gson, isPinned: Boolean = false): CachedRecipeEntity {
    return CachedRecipeEntity(
        id = id,
        title = title,
        image = image,
        readyInMinutes = readyInMinutes,
        summary = summary,
        cuisinesJson = gson.toJson(cuisines),
        ingredientsJson = gson.toJson(ingredients),
        instructionsJson = gson.toJson(instructions),
        cachedAtTimestamp = System.currentTimeMillis(),
        isPinnedOffline = isPinned
    )
}
