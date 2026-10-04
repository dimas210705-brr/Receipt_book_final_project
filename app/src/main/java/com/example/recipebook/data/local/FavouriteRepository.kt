package com.example.recipebook.data.local

import com.example.recipebook.models.Meal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepository @Inject constructor(
    private val favouriteDao: FavouriteDao
) {

    fun observeFavourites(): Flow<List<Meal>> =
        favouriteDao.observeAll().map { entities ->
            entities.map { it.toMeal() }
        }

    suspend fun addToFavourites(meal: Meal) {
        favouriteDao.insert(meal.toFavouriteEntity())
    }

    suspend fun removeFromFavourites(mealId: Int) {
        favouriteDao.deleteById(mealId)
    }
}
