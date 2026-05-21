package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    val ingredients: StateFlow<List<IngredientEntity>> = dao.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            dao.insertOrUpdateIngredient(ingredient)
        }
    }

    fun deleteIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            dao.deleteIngredient(ingredient)
        }
    }
}
