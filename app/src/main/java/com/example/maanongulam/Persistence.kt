package com.example.maanongulam

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * --- DATABASE CONFIGURATION ---
 * This class defines the database tables (entities) and the version.
 * It uses the Singleton pattern to ensure only one instance of the database exists.
 */
@Database(
    entities = [
        IngredientEntity::class, 
        RecipeEntity::class, 
        CookingLogEntity::class, 
        ShoppingItem::class, 
        FoodDefinitionEntity::class
    ],
    version = 14,
    exportSchema = false,
)
@TypeConverters(Converters::class) // Handles complex objects like List<Ingredient>
abstract class AppDatabase : RoomDatabase() {

    abstract fun maAnongUlamDao(): MaAnongUlamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton pattern: Returns the existing database or creates a new one
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ma_anong_ulam_database"
                )
                    .addCallback(AppDatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Database Callback: Runs when the database is first opened
        private class AppDatabaseCallback : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.maAnongUlamDao()
                        val recipes = dao.getAllRecipesSnapshot()
                        // If the database is empty, seed it with initial data
                        if (recipes.isEmpty()) {
                            seedDatabase(dao)
                        }
                    }
                }
            }

            /**
             * SEED DATABASE FUNCTION
             * Populates the app with default food types, ingredients, and recipes
             * during the first run or after a clear-data action.
             */
            suspend fun seedDatabase(dao: MaAnongUlamDao) {
                val oneWeekFromNow = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)

                // 1. Define Default Food Types (The master list for dropdowns)
                val defaultDefinitions = listOf(
                    FoodDefinitionEntity(name = "Chicken", unitType = "MASS", category = "Meat"),
                    FoodDefinitionEntity(name = "Pork", unitType = "MASS", category = "Meat"),
                    FoodDefinitionEntity(name = "Beef", unitType = "MASS", category = "Meat"),
                    FoodDefinitionEntity(name = "Liver", unitType = "MASS", category = "Meat"),
                    FoodDefinitionEntity(name = "Soy Sauce", unitType = "VOLUME", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Vinegar", unitType = "VOLUME", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Tomato Sauce", unitType = "VOLUME", category = "Spices"),
                    FoodDefinitionEntity(name = "Tamarind Mix", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Curry Powder", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Star Anise", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Sugar", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Garlic", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "Peppercorns", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Bay Leaves", unitType = "MASS", category = "Spices", isImperishable = true),
                    FoodDefinitionEntity(name = "Radish", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "Eggplant", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "String Beans", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "Potato", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "Carrot", unitType = "MASS", category = "Vegetables"),
                    FoodDefinitionEntity(name = "Egg", unitType = "MASS", category = "Dairy"),
                    FoodDefinitionEntity(name = "Milk", unitType = "VOLUME", category = "Dairy"),
                    FoodDefinitionEntity(name = "Coconut Milk", unitType = "VOLUME", category = "Dairy"),
                    FoodDefinitionEntity(name = "Rice", unitType = "MASS", category = "Grains", isImperishable = true),
                    FoodDefinitionEntity(name = "Water", unitType = "VOLUME", category = "Others", isImperishable = true)
                )
                defaultDefinitions.forEach { dao.insertFoodDefinition(it) }

                // 2. Add Initial Ingredients to Inventory
                val defaultIngredients = listOf(
                    IngredientEntity(name = "Chicken", quantity = 1000.0, unit = "g", expirationDate = oneWeekFromNow, category = "Meat"),
                    IngredientEntity(name = "Soy Sauce", quantity = 500.0, unit = "ml", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Vinegar", quantity = 500.0, unit = "ml", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Garlic", quantity = 100.0, unit = "g", expirationDate = oneWeekFromNow, category = "Vegetables"),
                    IngredientEntity(name = "Peppercorns", quantity = 50.0, unit = "g", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Bay Leaves", quantity = 10.0, unit = "g", expirationDate = oneWeekFromNow, category = "Spices")
                )
                defaultIngredients.forEach { dao.insertOrUpdateIngredient(it) }

                // 3. Add Default Filipino Recipes
                dao.insertRecipe(RecipeEntity(name = "Chicken Adobo", ingredients = listOf(
                    Ingredient("Chicken", 500.0, "g", 0L),
                    Ingredient("Soy Sauce", 100.0, "ml", 0L),
                    Ingredient("Vinegar", 50.0, "ml", 0L),
                    Ingredient("Garlic", 20.0, "g", 0L),
                    Ingredient("Peppercorns", 5.0, "g", 0L),
                    Ingredient("Bay Leaves", 2.0, "g", 0L)
                )))

                dao.insertRecipe(RecipeEntity(name = "Sinigang na Baboy", ingredients = listOf(
                    Ingredient("Pork", 500.0, "g", 0L),
                    Ingredient("Tamarind Mix", 1.0, "g", 0L),
                    Ingredient("Radish", 100.0, "g", 0L),
                    Ingredient("Eggplant", 100.0, "g", 0L),
                    Ingredient("String Beans", 50.0, "g", 0L),
                    Ingredient("Water", 1.5, "L", 0L)
                )))

                dao.insertRecipe(RecipeEntity(name = "Chicken Curry", ingredients = listOf(
                    Ingredient("Chicken", 500.0, "g", 0L),
                    Ingredient("Curry Powder", 20.0, "g", 0L),
                    Ingredient("Coconut Milk", 200.0, "ml", 0L),
                    Ingredient("Potato", 150.0, "g", 0L),
                    Ingredient("Carrot", 100.0, "g", 0L)
                )))

                dao.insertRecipe(RecipeEntity(name = "Pork Menudo", ingredients = listOf(
                    Ingredient("Pork", 500.0, "g", 0L),
                    Ingredient("Tomato Sauce", 200.0, "ml", 0L),
                    Ingredient("Liver", 100.0, "g", 0L),
                    Ingredient("Potato", 100.0, "g", 0L),
                    Ingredient("Carrot", 100.0, "g", 0L)
                )))

                dao.insertRecipe(RecipeEntity(name = "Beef Pares", ingredients = listOf(
                    Ingredient("Beef", 500.0, "g", 0L),
                    Ingredient("Star Anise", 2.0, "g", 0L),
                    Ingredient("Soy Sauce", 100.0, "ml", 0L),
                    Ingredient("Sugar", 50.0, "g", 0L),
                    Ingredient("Garlic", 20.0, "g", 0L)
                )))
            }
        }
    }
}

/**
 * --- DATA ACCESS OBJECT (DAO) ---
 * Defines the SQL queries used to interact with the database.
 * Room automatically implements these functions.
 */
@Dao
interface MaAnongUlamDao {
    // Inventory Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE name = :name")
    suspend fun getIngredientsByName(name: String): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun getIngredientByName(name: String): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Int): IngredientEntity?

    @Query("DELETE FROM ingredients")
    suspend fun deleteAllIngredients()

    // Recipe Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesSnapshot(): List<RecipeEntity>

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    // Cooking History Operations
    @Insert
    suspend fun insertCookingLog(log: CookingLogEntity)

    @Query("SELECT * FROM cooking_logs ORDER BY timestamp DESC")
    fun getAllCookingLogs(): Flow<List<CookingLogEntity>>

    @Query("SELECT * FROM cooking_logs ORDER BY timestamp DESC")
    suspend fun getAllCookingLogsOnce(): List<CookingLogEntity>

    @Delete
    suspend fun deleteCookingLog(log: CookingLogEntity)

    @Query("DELETE FROM cooking_logs")
    suspend fun deleteAllCookingLogs()

    // Shopping List Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Query("SELECT * FROM shopping_list WHERE name = :name LIMIT 1")
    suspend fun getShoppingItemByName(name: String): ShoppingItem?

    @Query("SELECT * FROM shopping_list")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    // Master Food Definitions (Lookups)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodDefinition(definition: FoodDefinitionEntity)

    @Query("SELECT * FROM food_definitions ORDER BY name ASC")
    fun getAllFoodDefinitions(): Flow<List<FoodDefinitionEntity>>

    @Query("SELECT * FROM food_definitions WHERE name = :name LIMIT 1")
    suspend fun getFoodDefinitionByName(name: String): FoodDefinitionEntity?

    @Delete
    suspend fun deleteFoodDefinition(definition: FoodDefinitionEntity)
}

/**
 * --- ROOM TYPE CONVERTERS ---
 * Room can't store complex objects like List<Ingredient> directly.
 * These converters turn lists into JSON strings for storage and back again.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>): String = gson.toJson(value)

    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType)
    }
}
