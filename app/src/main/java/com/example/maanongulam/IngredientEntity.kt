package com.example.maanongulam

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing ingredients in the local database.
 */
@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String = "g", // Defaults to grams (Philippine metric standard)
    val expirationDate: Long
)

/**
 * Extension function to convert Entity to Domain model.
 */
fun IngredientEntity.toDomainModel() = Ingredient(
    name = name,
    quantity = quantity,
    unit = unit,
    expirationDate = expirationDate
)
