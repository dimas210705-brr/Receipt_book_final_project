package com.example.recipebook.vm

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
import com.example.recipebook.models.SearchFilter
import com.example.recipebook.testutil.MainDispatcherRule
import com.example.recipebook.ui.viewmodel.RecipeBookViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecipeBookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeApi = FakeMealApi()
    private val fakeFavouriteDao = FakeFavouriteDao()

    private fun createViewModel(): RecipeBookViewModel =
        RecipeBookViewModel(
            mealRepository = MealRepository(fakeApi),
            favouriteRepository = FavouriteRepository(fakeFavouriteDao)
        )

    private fun advanceWork() {
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun initialState_hasCorrectDefaults() {
        val vm = createViewModel()
        advanceWork()
        with(vm.uiState.value) {
            assertEquals("", query)
            assertEquals(SearchFilter.ALL, filter)
            assertTrue(searchResults.isEmpty())
            assertFalse(isSearchLoading)
            assertNull(searchError)
            assertNull(selectedRecipe)
            assertFalse(isRecipeLoading)
            assertNull(recipeError)
            assertTrue(favourites.isEmpty())
        }
    }

    @Test
    fun searchMeals_withDebounce_updatesSearchResults() {
        fakeApi.searchResponse = MealSearchResponse(
            listOf(
                MealSearchResult(id = 1, image = "img1.jpg", title = "Pasta Carbonara"),
                MealSearchResult(id = 2, image = "img2.jpg", title = "Pasta Bolognese")
            )
        )
        val vm = createViewModel()
        vm.updateSearchQuery("pasta")

        // Проверяем debounce
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(200)
        assertTrue(vm.uiState.value.searchResults.isEmpty())

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(300)
        advanceWork()

        assertFalse(vm.uiState.value.isSearchLoading)
        assertNull(vm.uiState.value.searchError)
        assertTrue(vm.uiState.value.hasCompletedSearch)
        assertEquals(2, vm.uiState.value.searchResults.size)
        assertEquals("Pasta Carbonara", vm.uiState.value.searchResults[0].title)
    }

    @Test
    fun filterOnlyFavourites_reactivelyFiltersListWithoutNewNetworkCall() {
        fakeApi.searchResponse = MealSearchResponse(
            listOf(
                MealSearchResult(id = 1, image = "img1.jpg", title = "Pasta Carbonara"),
                MealSearchResult(id = 2, image = "img2.jpg", title = "Pasta Bolognese")
            )
        )
        val vm = createViewModel()
        vm.updateSearchQuery("pasta")
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        advanceWork()

        assertEquals(2, vm.uiState.value.searchResults.size)

        vm.toggleFavourite(1)
        advanceWork()

        val callCountBeforeFilter = fakeApi.searchCallCount

        vm.updateFilter(SearchFilter.ONLY_FAVOURITES)
        advanceWork()

        assertEquals(1, vm.uiState.value.searchResults.size)
        assertEquals(1, vm.uiState.value.searchResults[0].id)
        assertEquals(callCountBeforeFilter, fakeApi.searchCallCount)
    }

    @Test
    fun searchMeals_failure_setsSearchError() {
        fakeApi.shouldThrow = true
        val vm = createViewModel()
        vm.updateSearchQuery("pasta")
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(500)
        advanceWork()

        assertFalse(vm.uiState.value.isSearchLoading)
        assertNotNull(vm.uiState.value.searchError)
        assertFalse(vm.uiState.value.hasCompletedSearch)
        assertTrue(vm.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun loadRecipe_success_updatesSelectedRecipe() {
        fakeApi.recipeInfoResponse = sampleRecipeInfoResponse(id = 42)
        fakeApi.instructionsResponse = sampleInstructionsResponse()
        val vm = createViewModel()

        vm.loadRecipe(42)
        advanceWork()

        assertFalse(vm.uiState.value.isRecipeLoading)
        assertNull(vm.uiState.value.recipeError)
        assertNotNull(vm.uiState.value.selectedRecipe)
        assertEquals(42, vm.uiState.value.selectedRecipe!!.id)
    }

    @Test
    fun retryLoadRecipe_triggersNewApiCall() {
        fakeApi.shouldThrow = true
        val vm = createViewModel()

        vm.loadRecipe(42)
        advanceWork()

        assertNotNull(vm.uiState.value.recipeError)
        val callsAfterError = fakeApi.getRecipeInfoCallCount

        fakeApi.shouldThrow = false
        fakeApi.recipeInfoResponse = sampleRecipeInfoResponse(id = 42)
        fakeApi.instructionsResponse = sampleInstructionsResponse()

        vm.retryLoadRecipe()
        advanceWork()

        assertTrue(fakeApi.getRecipeInfoCallCount > callsAfterError)
        assertNotNull(vm.uiState.value.selectedRecipe)
    }

    private fun sampleRecipeInfoResponse(id: Int) = RecipeInformationResponse(
        id = id,
        title = "Test Recipe",
        image = "test.jpg",
        readyInMinutes = 30,
        summary = "A delicious test recipe.",
        cuisines = listOf("Italian"),
        extendedIngredients = listOf(
            ExtendedIngredient(name = "Flour", amount = 2.0, unit = "cups")
        )
    )

    private fun sampleInstructionsResponse() = listOf(
        AnalyzedInstructionResponse(
            name = "",
            steps = listOf(StepResponse(number = 1, step = "Mix ingredients"))
        )
    )
}

class FakeMealApi : MealApi {
    var searchResponse = MealSearchResponse(emptyList())
    var recipeInfoResponse: RecipeInformationResponse? = null
    var instructionsResponse: List<AnalyzedInstructionResponse> = emptyList()
    var shouldThrow = false
    var searchCallCount = 0
    var getRecipeInfoCallCount = 0

    override suspend fun searchMeals(query: String): MealSearchResponse {
        searchCallCount++
        if (shouldThrow) throw RuntimeException("Network error")
        return searchResponse
    }

    override suspend fun getRecipeInformation(id: Int): RecipeInformationResponse {
        getRecipeInfoCallCount++
        if (shouldThrow) throw RuntimeException("Network error")
        return recipeInfoResponse ?: throw RuntimeException("Not found")
    }

    override suspend fun getAnalyzedInstructions(id: Int): List<AnalyzedInstructionResponse> {
        if (shouldThrow) throw RuntimeException("Network error")
        return instructionsResponse
    }
}

class FakeFavouriteDao : FavouriteDao {
    private val items = MutableStateFlow<List<FavouriteMealEntity>>(emptyList())

    override fun observeAll(): Flow<List<FavouriteMealEntity>> = items

    override suspend fun insert(meal: FavouriteMealEntity) {
        items.value = items.value.filter { it.id != meal.id } + meal
    }

    override suspend fun deleteById(mealId: Int) {
        items.value = items.value.filter { it.id != mealId }
    }
}
