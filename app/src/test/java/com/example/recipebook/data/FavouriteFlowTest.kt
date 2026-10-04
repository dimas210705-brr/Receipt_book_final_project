package com.example.recipebook.data

import com.example.recipebook.data.local.FavouriteDao
import com.example.recipebook.data.local.FavouriteMealEntity
import com.example.recipebook.data.local.FavouriteRepository
import com.example.recipebook.models.Meal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouriteFlowTest {

    @Test
    fun observeFavourites_emitsFullSequenceOnChanges() = runTest {
        val dao = InMemoryFavouriteDao()
        val repo = FavouriteRepository(dao)

        val emissions = mutableListOf<List<Meal>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            repo.observeFavourites().collect { emissions.add(it) }
        }

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())

        repo.addToFavourites(Meal(id = 1, title = "Pasta", image = "img1.jpg"))
        assertEquals(2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("Pasta", emissions[1][0].title)

        repo.addToFavourites(Meal(id = 2, title = "Salad", image = "img2.jpg"))
        assertEquals(3, emissions.size)
        assertEquals(2, emissions[2].size)

        repo.removeFromFavourites(1)
        assertEquals(4, emissions.size)
        assertEquals(1, emissions[3].size)
        assertEquals("Salad", emissions[3][0].title)

        collectJob.cancel()
    }

    @Test
    fun insertDuplicate_doesNotCreateDuplicateInEmission() = runTest {
        val dao = InMemoryFavouriteDao()
        val repo = FavouriteRepository(dao)

        val emissions = mutableListOf<List<Meal>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            repo.observeFavourites().collect { emissions.add(it) }
        }

        assertEquals("initial empty emission", 1, emissions.size)
        assertTrue(emissions[0].isEmpty())

        repo.addToFavourites(Meal(id = 1, title = "Pasta", image = "img.jpg"))

        assertEquals("emission after first insert", 2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("Pasta", emissions[1][0].title)

        repo.addToFavourites(Meal(id = 1, title = "Pasta Updated", image = "img.jpg"))

        assertEquals("emission after duplicate insert", 3, emissions.size)
        assertEquals(1, emissions[2].size)
        assertEquals("Pasta Updated", emissions[2][0].title)

        collectJob.cancel()
    }
}

private class InMemoryFavouriteDao : FavouriteDao {
    private val items = MutableStateFlow<List<FavouriteMealEntity>>(emptyList())

    override fun observeAll(): Flow<List<FavouriteMealEntity>> = items

    override suspend fun insert(meal: FavouriteMealEntity) {
        items.value = items.value.filter { it.id != meal.id } + meal
    }

    override suspend fun deleteById(mealId: Int) {
        items.value = items.value.filter { it.id != mealId }
    }
}
