package com.example.maanongulam

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// --- Domain Models (Used in UI and Logic) ---

/**
 * Represents a food ingredient with its quantity and expiration information.
 */
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: String = "g",
    val expirationDate: Long,
    val category: String = "Others"
)

/**
 * Represents a food recipe.
 */
data class Recipe(
    val name: String,
    val ingredients: List<Ingredient>
)

/**
 * Data class representing a recipe recommendation result.
 */
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean,
    val reasons: List<String> = emptyList(),
    val hasSpoiledIngredients: Boolean = false,
    val missingIngredients: List<Ingredient> = emptyList()
)

// --- Room Entities (Database Tables) ---

/**
 * Room Entity for storing ingredients in the local database.
 */
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val quantity: Double,
    val unit: String = "g", // Defaults to grams (Philippine metric standard)
    val expirationDate: Long,
    val category: String = "Others"
)

/**
 * Room Entity for storing recipes.
 * The ingredients list is handled by your TypeConverters.
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
 * Room Entity for tracking what has been cooked.
 */
@Entity(tableName = "cooking_logs")
data class CookingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeName: String,
    val timestamp: Long
)

/**
 * Room Entity for the shopping list items.
 */
@Entity(tableName = "shopping_list")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val quantity: Double,
    val unit: String,
    val isChecked: Boolean = false
)

// --- Conversion Extensions ---

/**
 * Converts Ingredient Database Entity to Domain Model.
 */
fun IngredientEntity.toDomainModel() = Ingredient(
    name = name,
    quantity = quantity,
    unit = unit,
    expirationDate = expirationDate,
    category = category
)

/**
 * Converts Recipe Database Entity to Domain Model.
 */
fun RecipeEntity.toDomainModel() = Recipe(
    name = name,
    ingredients = ingredients
)

/**
 * Creates a Recipe Database Entity from a Domain Model.
 */
fun RecipeEntity.Companion.fromDomainModel(recipe: Recipe) = RecipeEntity(
    name = recipe.name,
    ingredients = recipe.ingredients
)