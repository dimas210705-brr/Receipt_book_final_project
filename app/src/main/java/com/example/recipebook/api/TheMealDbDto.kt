package com.example.recipebook.api

data class MealSearchResponse(
    val results: List<MealSearchResult>
)

data class MealSearchResult(
    val id: Int,
    val image: String,
    val title: String,
)

data class RecipeInformationResponse(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val summary: String,
    val cuisines: List<String>,
    val extendedIngredients: List<ExtendedIngredient>
)

data class ExtendedIngredient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class AnalyzedInstructionResponse(
    val name: String?,
    val steps: List<StepResponse>
)

data class StepResponse(
    val number: Int,
    val step: String
)
