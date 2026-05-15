package com.example.maanongulam

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing recipes.
 * The ingredients list is handled by [Converters].
 */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ingredients: List<Ingredient>
) {
    companion object
}

/**
 * Extension function to convert Entity to Domain model.
 */
fun RecipeEntity.toDomainModel() = Recipe(
    name = name,
    ingredients = ingredients
)

/**
 * Companion object for conversion.
 */
fun RecipeEntity.Companion.fromDomainModel(recipe: Recipe) = RecipeEntity(
    name = recipe.name,
    ingredients = recipe.ingredients
)