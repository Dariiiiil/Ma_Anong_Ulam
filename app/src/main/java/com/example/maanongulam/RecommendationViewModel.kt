package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    val allRecipes: StateFlow<List<RecipeEntity>> = dao.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val recommendations: StateFlow<List<RecommendedRecipe>> = combine(
        dao.getAllIngredients().map { entities -> entities.map { it.toDomainModel() } },
        allRecipes.map { entities -> entities.map { it.toDomainModel() } }
    ) { inventory, recipes ->
        if (inventory.isEmpty() || recipes.isEmpty()) emptyList()
        else RecipeRecommendationEngine.recommendTopRecipes(inventory, recipes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cookingLog = MutableStateFlow<List<String>?>(null)
    val cookingLog: StateFlow<List<String>?> = _cookingLog.asStateFlow()

    fun cookRecipe(recipe: Recipe) {
        viewModelScope.launch {
            val log = mutableListOf<String>()
            val shortages = mutableListOf<String>()
            
            // 1. First, check if we have enough of EVERYTHING
            recipe.ingredients.forEach { needed ->
                val current = dao.getIngredientByName(needed.name)
                if (current == null) {
                    shortages.add("❌ ${needed.name}: Missing entirely (Need ${needed.quantity}${needed.unit})")
                } else if (current.quantity < needed.quantity) {
                    val shortfall = (needed.quantity - current.quantity)
                    shortages.add("⚠️ ${needed.name}: Short by ${shortfall}${current.unit} (Have ${current.quantity}${current.unit}, Need ${needed.quantity}${current.unit})")
                }
            }

            if (shortages.isNotEmpty()) {
                // 2a. If anything is missing, STOP and don't change the database
                log.add("Cannot cook ${recipe.name}!")
                log.add("Your inventory is missing some items:")
                log.addAll(shortages)
                log.add("")
                log.add("Inventory was NOT updated. Please add more ingredients.")
            } else {
                // 2b. If everything is sufficient, proceed with cooking
                log.add("Cooking ${recipe.name}...")
                recipe.ingredients.forEach { needed ->
                    val current = dao.getIngredientByName(needed.name)!!
                    val remaining = current.quantity - needed.quantity
                    dao.deductIngredientQuantity(needed.name, needed.quantity)
                    log.add("✅ ${needed.name}: Used ${needed.quantity}${current.unit} (${remaining}${current.unit} remaining)")
                }
                log.add("")
                log.add("Enjoy your meal! Inventory updated.")
            }

            _cookingLog.value = log
        }
    }

    fun clearCookingLog() {
        _cookingLog.value = null
    }

    fun addRecipe(name: String, ingredients: List<Ingredient>) {
        viewModelScope.launch {
            dao.insertRecipe(RecipeEntity(name = name, ingredients = ingredients))
        }
    }

    fun updateRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            dao.insertRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            dao.deleteRecipe(recipe)
        }
    }
}
