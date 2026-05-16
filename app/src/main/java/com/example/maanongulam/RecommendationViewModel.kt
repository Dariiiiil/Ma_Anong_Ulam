package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    private val _allRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    
    val recommendations: StateFlow<List<RecommendedRecipe>> = combine(
        dao.getAllIngredients().map { entities -> entities.map { it.toDomainModel() } },
        _allRecipes
    ) { inventory, recipes ->
        if (inventory.isEmpty() || recipes.isEmpty()) emptyList()
        else RecipeRecommendationEngine.recommendTopRecipes(inventory, recipes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRecipes()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            val recipes = dao.getAllRecipes().map { it.toDomainModel() }
            _allRecipes.value = recipes
        }
    }

    fun cookRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipe.ingredients.forEach { ingredient ->
                dao.deductIngredientQuantity(ingredient.name, ingredient.quantity)
            }
        }
    }

    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            dao.insertRecipe(RecipeEntity.fromDomainModel(recipe))
            loadRecipes() // Refresh local list
        }
    }
}
