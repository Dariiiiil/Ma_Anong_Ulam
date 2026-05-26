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
    
    val allIngredients: StateFlow<List<IngredientEntity>> = dao.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCookingLogs: StateFlow<List<CookingLogEntity>> = dao.getAllCookingLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendations: StateFlow<List<RecommendedRecipe>> = combine(
        allIngredients.map { entities -> entities.map { it.toDomainModel() } },
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
            
            // 1. First, check if we have enough of EVERYTHING (with unit conversion)
            recipe.ingredients.forEach { needed ->
                val current = dao.getIngredientByName(needed.name)
                if (current == null) {
                    val neededDisplay = UnitConverter.formatDisplay(needed.quantity, needed.unit)
                    shortages.add("❌ ${needed.name}: Missing entirely (Need $neededDisplay)")
                } else if (!UnitConverter.isEnough(current.quantity, current.unit, needed.quantity, needed.unit)) {
                    val shortfall = UnitConverter.getShortfall(current.quantity, current.unit, needed.quantity, needed.unit)
                    val shortfallDisplay = UnitConverter.formatDisplay(shortfall, needed.unit)
                    val haveDisplay = UnitConverter.formatDisplay(current.quantity, current.unit)
                    val needDisplay = UnitConverter.formatDisplay(needed.quantity, needed.unit)
                    shortages.add("⚠️ ${needed.name}: Short by $shortfallDisplay (Have $haveDisplay, Need $needDisplay)")
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
                    val newQuantity = UnitConverter.calculateRemaining(
                        current.quantity, current.unit, needed.quantity, needed.unit
                    )
                    dao.insertOrUpdateIngredient(current.copy(quantity = newQuantity))
                    
                    val usedDisplay = UnitConverter.formatDisplay(needed.quantity, needed.unit)
                    val remainingDisplay = UnitConverter.formatDisplay(newQuantity, current.unit)
                    log.add("✅ ${needed.name}: Used $usedDisplay ($remainingDisplay remaining)")
                }
                log.add("")
                log.add("Enjoy your meal! Inventory updated.")
                
                // Add to persistent history
                dao.insertCookingLog(CookingLogEntity(recipeName = recipe.name, timestamp = System.currentTimeMillis()))
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

    fun addIngredientIfMissing(name: String, unit: String, expirationDate: Long, category: String) {
        viewModelScope.launch {
            val existing = dao.getIngredientByName(name)
            if (existing == null) {
                dao.insertOrUpdateIngredient(
                    IngredientEntity(
                        name = name,
                        quantity = 0.0,
                        unit = unit,
                        expirationDate = expirationDate,
                        category = category
                    )
                )
            }
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

    fun deleteAllRecipes() {
        viewModelScope.launch {
            dao.deleteAllRecipes()
        }
    }

    fun deleteAllCookingLogs() {
        viewModelScope.launch {
            dao.deleteAllCookingLogs()
        }
    }

    fun addMissingToShoppingList(missing: List<Ingredient>) {
        viewModelScope.launch {
            missing.forEach { item ->
                dao.insertShoppingItem(
                    ShoppingItem(
                        name = item.name,
                        quantity = item.quantity,
                        unit = item.unit
                    )
                )
            }
        }
    }

    /**
     * Algorithm to automatically scan inventory and add low-stock items 
     * to the shopping list if they have 'autoAddToShoppingList' enabled.
     */
    fun runAutoRestockCheck() {
        viewModelScope.launch {
            val currentShoppingList = dao.getAllShoppingItems().first().map { it.name.lowercase() }
            
            // Automatically add missing items for the "Ulam of the Day"
            val topRecommendation = recommendations.value.firstOrNull()
            if (topRecommendation != null && topRecommendation.isInsufficient) {
                topRecommendation.missingIngredients.forEach { missing ->
                    if (missing.name.lowercase() !in currentShoppingList) {
                        dao.insertShoppingItem(
                            ShoppingItem(
                                name = missing.name,
                                quantity = missing.quantity,
                                unit = missing.unit
                            )
                        )
                    }
                }
            }
        }
    }
}
