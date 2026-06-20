package com.example.maanongulam

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * --- DOMAIN MODELS ---
 * These are "clean" data classes used by the UI and the Recommendation Engine.
 * They represent data in a way that is easy for business logic to process.
 */

// Represents a food ingredient with its quantity and expiration information.
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: String = "g",
    val expirationDate: Long,
    val category: String = "Others"
)

// Represents a food recipe.
data class Recipe(
    val name: String,
    val ingredients: List<Ingredient>
)

// Represents a recipe recommendation result with scores and specific reasons.
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean,
    val reasons: List<String> = emptyList(),
    val hasSpoiledIngredients: Boolean = false,
    val missingIngredients: List<Ingredient> = emptyList()
)

/**
 * --- ROOM ENTITIES (DATABASE TABLES) ---
 * These classes define the actual structure of the database tables.
 * Each "Entity" corresponds to a table in the SQLite database.
 */

// Table for storing inventory batches.
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) // Makes searching case-insensitive
    val name: String,
    val quantity: Double,
    val unit: String = "g",
    val expirationDate: Long,
    val category: String = "Others"
)

// Table for storing user-defined recipes.
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ingredients: List<Ingredient>
) {
    companion object
}

// Table for tracking cooking history.
@Entity(tableName = "cooking_logs")
data class CookingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeName: String,
    val timestamp: Long
)

// Master lookup table for food types (Category, Mass vs Volume).
@Entity(tableName = "food_definitions")
data class FoodDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val unitType: String, // "MASS" or "VOLUME"
    val category: String = "Others",
    val isImperishable: Boolean = false
)

// Table for the shopping list items.
@Entity(tableName = "shopping_list")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val quantity: Double,
    val unit: String,
    val isChecked: Boolean = false
)

/**
 * --- CONVERSION EXTENSIONS ---
 * These helper functions convert Database Entities to Domain Models.
 * This keeps the UI logic separate from the Database structure (Separation of Concerns).
 */

fun IngredientEntity.toDomainModel() = Ingredient(
    name = name,
    quantity = quantity,
    unit = unit,
    expirationDate = expirationDate,
    category = category
)

fun RecipeEntity.toDomainModel() = Recipe(
    name = name,
    ingredients = ingredients
)

fun RecipeEntity.Companion.fromDomainModel(recipe: Recipe) = RecipeEntity(
    name = recipe.name,
    ingredients = recipe.ingredients
)
