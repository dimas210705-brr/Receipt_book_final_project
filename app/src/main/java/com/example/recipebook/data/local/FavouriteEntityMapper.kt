package com.example.recipebook.data.local

import com.example.recipebook.models.Meal

fun FavouriteMealEntity.toMeal(): Meal = Meal(
    id = id,
    title = title,
    image = image
)

fun Meal.toFavouriteEntity(): FavouriteMealEntity = FavouriteMealEntity(
    id = id,
    title = title,
    image = image
)
