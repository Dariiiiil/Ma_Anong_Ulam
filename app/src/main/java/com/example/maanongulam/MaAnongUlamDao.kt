package com.example.maanongulam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MaAnongUlamDao {

    // --- Ingredients ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIngredient(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getIngredientByName(name: String): IngredientEntity?

    @Transaction
    suspend fun deductIngredientQuantity(name: String, amountToDeduct: Double) {
        val ingredient = getIngredientByName(name)
        if (ingredient != null) {
            val newQuantity = (ingredient.quantity - amountToDeduct).coerceAtLeast(0.0)
            insertOrUpdateIngredient(ingredient.copy(quantity = newQuantity))
        }
    }

    // --- Recipes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<RecipeEntity>
}
