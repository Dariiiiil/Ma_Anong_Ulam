package com.example.maanongulam

/**
 * Represents a food recipe.
 *
 * @property name The name of the recipe.
 * @property ingredients A list of [Ingredient] required for the recipe.
 */
data class Recipe(
    val name: String,
    val ingredients: List<Ingredient>
)
