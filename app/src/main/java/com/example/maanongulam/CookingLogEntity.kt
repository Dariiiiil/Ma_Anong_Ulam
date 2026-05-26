package com.example.maanongulam

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cooking_logs")
data class CookingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeName: String,
    val timestamp: Long
)
