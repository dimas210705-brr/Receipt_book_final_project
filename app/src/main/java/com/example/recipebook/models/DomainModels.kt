package com.example.recipebook.models

data class Meal(
    val id: Int,
    val title: String,
    val image: String,
)

data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class Instruction(
    val name: String?,
    val steps: List<Step>
)
data class Step(
    val number: Int,
    val step: String
)

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val summary: String,
    val cuisines: List<String>,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>
)

