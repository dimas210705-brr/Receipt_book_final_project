package com.example.recipebook.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MealApi{
    @GET("complexSearch")
    suspend fun searchMeals(
        @Query("query")
        query: String,
    ): MealSearchResponse

    @GET("{id}/information")
    suspend fun getRecipeInformation(
        @Path("id")
        id: Int,
    ): RecipeInformationResponse

    @GET("{id}/analyzedInstructions")
    suspend fun getAnalyzedInstructions(
        @Path("id")
        id: Int
    ): List<AnalyzedInstructionResponse>
}
