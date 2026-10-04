package com.example.recipebook

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recipebook.api.AnalyzedInstructionResponse
import com.example.recipebook.api.ExtendedIngredient
import com.example.recipebook.api.MealApi
import com.example.recipebook.api.MealRepository
import com.example.recipebook.api.MealSearchResponse
import com.example.recipebook.api.MealSearchResult
import com.example.recipebook.api.RecipeInformationResponse
import com.example.recipebook.api.StepResponse
import com.example.recipebook.data.local.FavouriteDao
import com.example.recipebook.data.local.FavouriteMealEntity
import com.example.recipebook.data.local.FavouriteRepository
import com.example.recipebook.ui.screen.RecipeDetailScreen
import com.example.recipebook.ui.screen.SearchScreen
import com.example.recipebook.ui.viewmodel.RecipeBookViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectingMealFromSearch_navigatesToDetailWithCorrectRecipe() {
        val fakeApi = object : MealApi {
            override suspend fun searchMeals(query: String) = MealSearchResponse(
                listOf(MealSearchResult(id = 42, image = "", title = "Test Pasta"))
            )

            override suspend fun getRecipeInformation(id: Int) = RecipeInformationResponse(
                id = id,
                title = "Test Pasta Detail",
                image = "",
                readyInMinutes = 20,
                summary = "Detailed recipe",
                cuisines = listOf("Italian"),
                extendedIngredients = listOf(
                    ExtendedIngredient(name = "Flour", amount = 2.0, unit = "cups")
                )
            )

            override suspend fun getAnalyzedInstructions(id: Int) = listOf(
                AnalyzedInstructionResponse(
                    name = "",
                    steps = listOf(StepResponse(number = 1, step = "Cook it"))
                )
            )
        }

        val fakeDao = object : FavouriteDao {
            private val items = MutableStateFlow<List<FavouriteMealEntity>>(emptyList())
            override fun observeAll(): Flow<List<FavouriteMealEntity>> = items
            override suspend fun insert(meal: FavouriteMealEntity) {
                items.value = items.value.filter { it.id != meal.id } + meal
            }
            override suspend fun deleteById(mealId: Int) {
                items.value = items.value.filter { it.id != mealId }
            }
        }

        val viewModel = RecipeBookViewModel(
            mealRepository = MealRepository(fakeApi),
            favouriteRepository = FavouriteRepository(fakeDao)
        )

        composeTestRule.setContent {
            val navController = rememberNavController()
            val uiState by viewModel.uiState.collectAsState()

            NavHost(navController = navController, startDestination = Screen.Search.route) {
                composable(route = Screen.Search.route) {
                    SearchScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::updateSearchQuery,
                        onFilterChange = viewModel::updateFilter,
                        onSearch = viewModel::searchMeals,
                        onMealClick = { meal ->
                            navController.navigate(Screen.Detail.createRoute(meal.id))
                        },
                        onNavigateToFavourites = {},
                        onToggleFavourite = viewModel::toggleFavourite
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(
                        navArgument("mealId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val mealId = backStackEntry.arguments?.getInt("mealId")
                    if (mealId != null) {
                        LaunchedEffect(mealId) {
                            viewModel.loadRecipe(mealId)
                        }
                    }
                    RecipeDetailScreen(
                        uiState = uiState,
                        onBackClick = {
                            viewModel.clearSelection()
                            navController.popBackStack()
                        },
                        onRetry = viewModel::retryLoadRecipe,
                        onToggleFavourite = {
                            mealId?.let { viewModel.toggleFavourite(it) }
                        }
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            viewModel.updateSearchQuery("pasta")
            viewModel.searchMeals()
        }

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.searchResults.isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Test Pasta")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.selectedRecipe != null
        }

        composeTestRule
            .onNodeWithText("Test Pasta Detail", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Flour", substring = true)
            .assertIsDisplayed()
    }
}
