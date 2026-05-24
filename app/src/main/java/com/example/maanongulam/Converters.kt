package com.example.maanongulam

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room to handle complex data types like Lists.
 */
class Converters {
    private val gson = Gson()

    @Suppress("unused")
    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>): String {
        return gson.toJson(value)
    }

    @Suppress("unused")
    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType)
    }
}
