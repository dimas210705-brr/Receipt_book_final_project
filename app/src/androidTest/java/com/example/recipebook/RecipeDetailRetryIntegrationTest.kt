package com.example.recipebook

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.recipebook.api.AnalyzedInstructionResponse
import com.example.recipebook.api.MealApi
import com.example.recipebook.api.MealRepository
import com.example.recipebook.api.MealSearchResponse
import com.example.recipebook.api.RecipeInformationResponse
import com.example.recipebook.data.local.FavouriteDao
import com.example.recipebook.data.local.FavouriteMealEntity
import com.example.recipebook.data.local.FavouriteRepository
import com.example.recipebook.ui.screen.RecipeDetailScreen
import com.example.recipebook.ui.viewmodel.RecipeBookViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class RecipeDetailRetryIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun retryFromError_triggersNewRequestAndShowsRecipe() {
        val fakeApi = object : MealApi {
            var shouldThrow = true
            var callCount = 0

            override suspend fun searchMeals(query: String) =
                MealSearchResponse(emptyList())

            override suspend fun getRecipeInformation(id: Int): RecipeInformationResponse {
                callCount++
                if (shouldThrow) throw RuntimeException("Network error")
                return RecipeInformationResponse(
                    id = id,
                    title = "Loaded Recipe",
                    image = "",
                    readyInMinutes = 30,
                    summary = "Tasty",
                    cuisines = emptyList(),
                    extendedIngredients = emptyList()
                )
            }

            override suspend fun getAnalyzedInstructions(id: Int): List<AnalyzedInstructionResponse> {
                if (shouldThrow) throw RuntimeException("Network error")
                return emptyList()
            }
        }

        val fakeDao = object : FavouriteDao {
            private val items = MutableStateFlow<List<FavouriteMealEntity>>(emptyList())
            override fun observeAll(): Flow<List<FavouriteMealEntity>> = items
            override suspend fun insert(meal: FavouriteMealEntity) {}
            override suspend fun deleteById(mealId: Int) {}
        }

        val viewModel = RecipeBookViewModel(
            mealRepository = MealRepository(fakeApi),
            favouriteRepository = FavouriteRepository(fakeDao)
        )

        composeTestRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            RecipeDetailScreen(
                uiState = uiState,
                onBackClick = {},
                onRetry = { viewModel.retryLoadRecipe() },
                onToggleFavourite = {}
            )
        }

        composeTestRule.runOnIdle {
            viewModel.loadRecipe(1)
        }

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.recipeError != null
        }

        composeTestRule
            .onNodeWithText("Не удалось загрузить рецепт. Повторите попытку позже.")
            .assertIsDisplayed()

        val callsAfterError = fakeApi.callCount

        fakeApi.shouldThrow = false

        composeTestRule
            .onNodeWithText("Повторить")
            .performClick()

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.selectedRecipe != null
        }

        assert(fakeApi.callCount > callsAfterError) {
            "retry() must trigger a new API request"
        }

        composeTestRule
            .onNodeWithText("Loaded Recipe", substring = true)
            .assertIsDisplayed()
    }
}
