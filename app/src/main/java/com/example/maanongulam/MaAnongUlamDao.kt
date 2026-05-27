package com.example.maanongulam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaAnongUlamDao {

    // --- Ingredients ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIngredient(ingredient: IngredientEntity)

    @androidx.room.Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getIngredientByName(name: String): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun getIngredientsByName(name: String): List<IngredientEntity>

    @Query("DELETE FROM ingredients")
    suspend fun deleteAllIngredients()

    // --- Recipes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @androidx.room.Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesSnapshot(): List<RecipeEntity>

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    // --- Cooking Logs ---

    @Insert
    suspend fun insertCookingLog(log: CookingLogEntity)

    @Query("SELECT * FROM cooking_logs ORDER BY timestamp DESC")
    fun getAllCookingLogs(): Flow<List<CookingLogEntity>>

    @Query("DELETE FROM cooking_logs")
    suspend fun deleteAllCookingLogs()

    // --- Shopping List ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Query("SELECT * FROM shopping_list")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @androidx.room.Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_list")
    suspend fun deleteAllShoppingItems()
}
