package com.example.recipebook

import com.example.recipebook.api.AnalyzedInstructionResponse
import com.example.recipebook.api.ExtendedIngredient
import com.example.recipebook.api.MealApi
import com.example.recipebook.api.MealSearchResponse
import com.example.recipebook.api.RecipeInformationResponse
import com.example.recipebook.data.local.CachedRecipeEntity
import com.example.recipebook.data.local.OfflineRecipeDao
import com.example.recipebook.data.preferences.UserSettings
import com.example.recipebook.data.preferences.UserSettingsRepository
import com.example.recipebook.data.repository.OfflineFirstRecipeRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineFirstRepositoryTest {

    private lateinit var fakeApi: FakeMealApiForOfflineTest
    private lateinit var fakeDao: FakeOfflineRecipeDao
    private lateinit var fakeSettingsRepo: FakeUserSettingsRepo
    private lateinit var repository: OfflineFirstRecipeRepository
    private val gson = Gson()

    @Before
    fun setUp() {
        fakeApi = FakeMealApiForOfflineTest()
        fakeDao = FakeOfflineRecipeDao()
        fakeSettingsRepo = FakeUserSettingsRepo()
        repository = OfflineFirstRecipeRepository(fakeApi, fakeDao, fakeSettingsRepo, gson)
    }

    @Test
    fun getCachedRecipe_returnsMappedDomainRecipe() = runTest {
        val entity = CachedRecipeEntity(
            id = 10,
            title = "Cached Pizza",
            image = "img.jpg",
            readyInMinutes = 25,
            summary = "Hot pizza",
            cuisinesJson = "[\"Italian\"]",
            ingredientsJson = "[]",
            instructionsJson = "[]",
            cachedAtTimestamp = System.currentTimeMillis()
        )
        fakeDao.insertRecipe(entity)

        val result = repository.getCachedRecipe(10)
        assertNotNull(result)
        assertEquals("Cached Pizza", result!!.title)
        assertEquals(listOf("Italian"), result.cuisines)
    }

    @Test
    fun refreshRecipe_whenNetworkFails_returnsCachedFallback() = runTest {
        val entity = CachedRecipeEntity(
            id = 15,
            title = "Offline Soup",
            image = "soup.jpg",
            readyInMinutes = 10,
            summary = "Hot soup",
            cuisinesJson = "[]",
            ingredientsJson = "[]",
            instructionsJson = "[]",
            cachedAtTimestamp = 0L // Кэш устарел (просрочен по TTL)
        )
        fakeDao.insertRecipe(entity)
        fakeApi.shouldThrow = true // Эмулируем отсутствие интернета

        val result = repository.refreshRecipe(15, force = true)
        assertTrue(result.isSuccess)
        assertEquals("Offline Soup", result.getOrNull()?.title)
    }

    @Test
    fun refreshRecipe_whenNetworkSuccess_updatesDao() = runTest {
        fakeApi.recipeResponse = RecipeInformationResponse(
            id = 20,
            title = "Fresh Pasta",
            image = "pasta.jpg",
            readyInMinutes = 15,
            summary = "Freshly cooked",
            cuisines = listOf("Italian"),
            extendedIngredients = listOf(
                ExtendedIngredient(name = "Pasta", amount = 100.0, unit = "g")
            )
        )

        val result = repository.refreshRecipe(20, force = true)
        assertTrue(result.isSuccess)
        assertEquals("Fresh Pasta", result.getOrNull()?.title)

        val cachedInDao = fakeDao.getRecipeById(20)
        assertNotNull(cachedInDao)
        assertEquals("Fresh Pasta", cachedInDao!!.title)
    }
}

class FakeOfflineRecipeDao : OfflineRecipeDao {
    private val storage = mutableMapOf<Int, CachedRecipeEntity>()
    private val flow = MutableStateFlow<CachedRecipeEntity?>(null)

    override fun observeRecipeById(id: Int): Flow<CachedRecipeEntity?> {
        flow.value = storage[id]
        return flow
    }

    override suspend fun getRecipeById(id: Int): CachedRecipeEntity? = storage[id]

    override suspend fun insertRecipe(recipe: CachedRecipeEntity) {
        storage[recipe.id] = recipe
        flow.value = recipe
    }

    override suspend fun clearUnpinnedCache() {
        storage.entries.removeIf { !it.value.isPinnedOffline }
    }
}

class FakeUserSettingsRepo : UserSettingsRepository(forTest = true) {
    var currentSettings = UserSettings(cacheTtlHours = 24)
    override val settingsFlow = MutableStateFlow(currentSettings)
}

class FakeMealApiForOfflineTest : MealApi {
    var shouldThrow = false
    var recipeResponse: RecipeInformationResponse? = null

    override suspend fun searchMeals(query: String) = MealSearchResponse(emptyList())

    override suspend fun getRecipeInformation(id: Int): RecipeInformationResponse {
        if (shouldThrow) throw RuntimeException("No internet connection")
        return recipeResponse ?: RecipeInformationResponse(
            id = id,
            title = "Default Recipe",
            image = "",
            readyInMinutes = 20,
            summary = "",
            cuisines = emptyList(),
            extendedIngredients = emptyList()
        )
    }

    override suspend fun getAnalyzedInstructions(id: Int): List<AnalyzedInstructionResponse> {
        if (shouldThrow) throw RuntimeException("No internet connection")
        return emptyList()
    }
}