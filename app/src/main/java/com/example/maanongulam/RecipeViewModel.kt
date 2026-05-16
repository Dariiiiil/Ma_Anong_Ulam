package com.example.maanongulam

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel to manage recipes and recommendations.
 */
class RecipeViewModel : ViewModel() {

    // Current inventory of ingredients
    private val _inventory = MutableStateFlow<List<Ingredient>>(emptyList())
    val inventory: StateFlow<List<Ingredient>> = _inventory.asStateFlow()

    // All available recipes
    private val _allRecipes = MutableStateFlow<List<Recipe>>(emptyList())

    // Top 3 recommended recipes
    private val _recommendations = MutableStateFlow<List<RecommendedRecipe>>(emptyList())
    val recommendations: StateFlow<List<RecommendedRecipe>> = _recommendations.asStateFlow()

    /**
     * Updates the inventory and triggers a new recommendation.
     */
    fun updateInventory(newInventory: List<Ingredient>) {
        _inventory.value = newInventory
        refreshRecommendations()
    }

    /**
     * Updates the recipes and triggers a new recommendation.
     */
    fun updateRecipes(newRecipes: List<Recipe>) {
        _allRecipes.value = newRecipes
        refreshRecommendations()
    }

    /**
     * Triggers the recommendation engine logic.
     */
    private fun refreshRecommendations() {
        val inventoryList = _inventory.value
        val recipeList = _allRecipes.value

        if (inventoryList.isNotEmpty() && recipeList.isNotEmpty()) {
            // Trigger the Recommendation Engine
            _recommendations.value = RecipeRecommendationEngine.recommendTopRecipes(
                inventory = inventoryList,
                recipes = recipeList
            )
        }
    }
}
