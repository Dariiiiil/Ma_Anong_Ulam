package com.example.maanongulam

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main database class for the Ma Anong Ulam application.
 */
@Database(
    entities = [IngredientEntity::class, RecipeEntity::class],
    version = 4,
    exportSchema = false
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
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
                // Default Ingredients with 0 quantity
                val defaultIngredients = listOf(
                    IngredientEntity(name = "Chicken", quantity = 0.0, unit = "g", expirationDate = System.currentTimeMillis()),
                    IngredientEntity(name = "Soy Sauce", quantity = 0.0, unit = "ml", expirationDate = System.currentTimeMillis()),
                    IngredientEntity(name = "Vinegar", quantity = 0.0, unit = "ml", expirationDate = System.currentTimeMillis()),
                    IngredientEntity(name = "Garlic", quantity = 0.0, unit = "g", expirationDate = System.currentTimeMillis()),
                    IngredientEntity(name = "Peppercorns", quantity = 0.0, unit = "g", expirationDate = System.currentTimeMillis()),
                    IngredientEntity(name = "Bay Leaves", quantity = 0.0, unit = "g", expirationDate = System.currentTimeMillis())
                )
                
                defaultIngredients.forEach { dao.insertOrUpdateIngredient(it) }

                // Default Adobo Recipe
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
