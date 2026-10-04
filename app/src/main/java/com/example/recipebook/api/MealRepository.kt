package com.example.recipebook.api

import com.example.recipebook.models.Meal
import com.example.recipebook.models.Recipe
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val api: MealApi
) {

    suspend fun searchMeals(query: String): List<Meal> =
        api.searchMeals(query = query).results.map { it.toMeal() }

    suspend fun getRecipe(id: Int): Recipe = coroutineScope {
        val infoDeferred = async { api.getRecipeInformation(id) }
        val instructionsDeferred = async { api.getAnalyzedInstructions(id) }

        val info = infoDeferred.await()
        val instructions = instructionsDeferred.await()

        info.toRecipe(instructions)
    }
}
