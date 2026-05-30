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

// --- Room Database Configuration ---

@Database(
    entities = [IngredientEntity::class, RecipeEntity::class, CookingLogEntity::class, ShoppingItem::class],
    version = 9,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun maAnongUlamDao(): MaAnongUlamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        private class AppDatabaseCallback : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.maAnongUlamDao()
                        val recipes = dao.getAllRecipesSnapshot()
                        if (recipes.isEmpty()) {
                            seedDatabase(dao)
                        }
                    }
                }
            }

            suspend fun seedDatabase(dao: MaAnongUlamDao) {
                val oneWeekFromNow = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)

                val defaultIngredients = listOf(
                    IngredientEntity(name = "Chicken", quantity = 1000.0, unit = "g", expirationDate = oneWeekFromNow, category = "Meat"),
                    IngredientEntity(name = "Soy Sauce", quantity = 500.0, unit = "ml", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Vinegar", quantity = 500.0, unit = "ml", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Garlic", quantity = 100.0, unit = "g", expirationDate = oneWeekFromNow, category = "Vegetables"),
                    IngredientEntity(name = "Peppercorns", quantity = 50.0, unit = "g", expirationDate = oneWeekFromNow, category = "Spices"),
                    IngredientEntity(name = "Bay Leaves", quantity = 10.0, unit = "g", expirationDate = oneWeekFromNow, category = "Spices")
                )

                defaultIngredients.forEach { dao.insertOrUpdateIngredient(it) }

                val adoboIngredients = listOf(
                    Ingredient("Chicken", 500.0, "g", 0L),
                    Ingredient("Soy Sauce", 100.0, "ml", 0L),
                    Ingredient("Vinegar", 50.0, "ml", 0L),
                    Ingredient("Garlic", 20.0, "g", 0L),
                    Ingredient("Peppercorns", 5.0, "g", 0L),
                    Ingredient("Bay Leaves", 2.0, "g", 0L)
                )

                dao.insertRecipe(RecipeEntity(name = "Chicken Adobo", ingredients = adoboIngredients))
            }
        }
    }
}

// --- Data Access Object (DAO) ---

@Dao
interface MaAnongUlamDao {
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

    @Query("DELETE FROM ingredients")
    suspend fun deleteAllIngredients()

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

    @Insert
    suspend fun insertCookingLog(log: CookingLogEntity)

    @Query("SELECT * FROM cooking_logs ORDER BY timestamp DESC")
    fun getAllCookingLogs(): Flow<List<CookingLogEntity>>

    @Query("DELETE FROM cooking_logs")
    suspend fun deleteAllCookingLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Query("SELECT * FROM shopping_list WHERE name = :name LIMIT 1")
    suspend fun getShoppingItemByName(name: String): ShoppingItem?

    @Query("SELECT * FROM shopping_list")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)
}

// --- Room Type Converters ---

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