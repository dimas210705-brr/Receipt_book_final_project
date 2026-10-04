package com.example.recipebook.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    @Test
    fun mealSearchResult_toMeal_mapsAllFields() {
        val result = MealSearchResult(id = 42, image = "img.jpg", title = "Pasta")
        val meal = result.toMeal()

        assertEquals(42, meal.id)
        assertEquals("Pasta", meal.title)
        assertEquals("img.jpg", meal.image)
    }

    @Test
    fun recipeInformationResponse_toRecipe_mapsAllFields() {
        val info = RecipeInformationResponse(
            id = 1,
            title = "Carbonara",
            image = "photo.jpg",
            readyInMinutes = 25,
            summary = "Classic Italian pasta.",
            cuisines = listOf("Italian", "European"),
            extendedIngredients = listOf(
                ExtendedIngredient(name = "Spaghetti", amount = 200.0, unit = "g"),
                ExtendedIngredient(name = "Egg", amount = 2.0, unit = "pcs")
            )
        )
        val instructions = listOf(
            AnalyzedInstructionResponse(
                name = "Main",
                steps = listOf(
                    StepResponse(number = 1, step = "Boil water"),
                    StepResponse(number = 2, step = "Cook pasta")
                )
            )
        )

        val recipe = info.toRecipe(instructions)

        assertEquals(1, recipe.id)
        assertEquals("Carbonara", recipe.title)
        assertEquals("photo.jpg", recipe.image)
        assertEquals(25, recipe.readyInMinutes)
        assertEquals("Classic Italian pasta.", recipe.summary)
        assertEquals(listOf("Italian", "European"), recipe.cuisines)
        assertEquals(2, recipe.ingredients.size)
        assertEquals("Spaghetti", recipe.ingredients[0].name)
        assertEquals(200.0, recipe.ingredients[0].amount, 0.001)
        assertEquals("g", recipe.ingredients[0].unit)
        assertEquals(1, recipe.instructions.size)
        assertEquals("Main", recipe.instructions[0].name)
        assertEquals(2, recipe.instructions[0].steps.size)
        assertEquals("Boil water", recipe.instructions[0].steps[0].step)
    }

    @Test
    fun recipeInformationResponse_toRecipe_withEmptyInstructionsAndIngredients() {
        val info = RecipeInformationResponse(
            id = 5,
            title = "Simple Dish",
            image = "",
            readyInMinutes = 10,
            summary = "",
            cuisines = emptyList(),
            extendedIngredients = emptyList()
        )

        val recipe = info.toRecipe(emptyList())

        assertEquals(5, recipe.id)
        assertTrue(recipe.ingredients.isEmpty())
        assertTrue(recipe.instructions.isEmpty())
        assertTrue(recipe.cuisines.isEmpty())
    }
}
