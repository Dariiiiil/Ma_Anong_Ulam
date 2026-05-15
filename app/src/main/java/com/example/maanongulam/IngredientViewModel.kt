package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    fun addIngredient(name: String, quantity: Double, unit: String, expirationDate: Long) {
        viewModelScope.launch {
            val entity = IngredientEntity(
                name = name,
                quantity = quantity,
                unit = unit,
                expirationDate = expirationDate
            )
            dao.insertOrUpdateIngredient(entity)
        }
    }
}