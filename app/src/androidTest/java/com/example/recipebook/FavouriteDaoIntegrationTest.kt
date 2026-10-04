package com.example.recipebook

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recipebook.data.local.FavouriteRepository
import com.example.recipebook.data.local.RecipeBookDatabase
import com.example.recipebook.models.Meal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavouriteDaoIntegrationTest {

    private lateinit var database: RecipeBookDatabase
    private lateinit var repository: FavouriteRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RecipeBookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FavouriteRepository(database.favouriteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addToFavourites_thenObserve_returnsAddedMeal() = runBlocking {
        val meal = Meal(id = 1, title = "Pasta", image = "img.jpg")

        repository.addToFavourites(meal)

        val favourites = repository.observeFavourites().first()
        assertEquals(1, favourites.size)
        assertEquals(meal.id, favourites[0].id)
        assertEquals(meal.title, favourites[0].title)
        assertEquals(meal.image, favourites[0].image)
    }

    @Test
    fun addDuplicate_updatesWithoutCreatingDuplicate() = runBlocking {
        repository.addToFavourites(Meal(id = 1, title = "Pasta", image = "img.jpg"))
        repository.addToFavourites(Meal(id = 1, title = "Pasta Updated", image = "img2.jpg"))

        val favourites = repository.observeFavourites().first()
        assertEquals(1, favourites.size)
        assertEquals("Pasta Updated", favourites[0].title)
        assertEquals("img2.jpg", favourites[0].image)
    }

    @Test
    fun addAndRemove_removesCorrectMeal() = runBlocking {
        repository.addToFavourites(Meal(id = 1, title = "Pasta", image = "img1.jpg"))
        repository.addToFavourites(Meal(id = 2, title = "Salad", image = "img2.jpg"))

        repository.removeFromFavourites(1)

        val favourites = repository.observeFavourites().first()
        assertEquals(1, favourites.size)
        assertEquals("Salad", favourites[0].title)
    }

    @Test
    fun removeAll_resultsInEmptyList() = runBlocking {
        repository.addToFavourites(Meal(id = 1, title = "Pasta", image = "img.jpg"))
        repository.addToFavourites(Meal(id = 2, title = "Salad", image = "img.jpg"))

        repository.removeFromFavourites(1)
        repository.removeFromFavourites(2)

        val favourites = repository.observeFavourites().first()
        assertTrue(favourites.isEmpty())
    }
}
