package com.example.maanongulam

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main database class for the Ma Anong Ulam application.
 */
@Database(
    entities = [IngredientEntity::class, RecipeEntity::class],
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}