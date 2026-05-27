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
            val currentTime = System.currentTimeMillis()
            val inventoryEntities = dao.getAllIngredients().first()
            val expirationBuffer = 60000L
            
            // 1. Check total usable quantity across all batches using robust normalization
            val recipeRequirements = recipe.ingredients.map { needed ->
                val normalizedNeededName = needed.name.replace("\\s".toRegex(), "").lowercase()
                val matchingBatches = inventoryEntities.filter { 
                    it.name.replace("\\s".toRegex(), "").lowercase() == normalizedNeededName 
                }
                
                // Only use non-spoiled batches
                val usableBatches = matchingBatches.filter { 
                    it.expirationDate == 0L || (it.expirationDate + expirationBuffer) >= currentTime 
                }
                
                val totalAvailableBase = usableBatches.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                val neededBase = UnitConverter.toBaseUnit(needed.quantity, needed.unit)
                
                Triple(needed, usableBatches, totalAvailableBase >= (neededBase - 0.01))
            }

            recipeRequirements.forEach { (needed, usableBatches, isSufficient) ->
                if (!isSufficient) {
                    val totalAvailableBase = usableBatches.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                    val shortfall = UnitConverter.getShortfall(totalAvailableBase, "g", needed.quantity, needed.unit)
                    val shortfallDisplay = UnitConverter.formatDisplay(shortfall, needed.unit)
                    shortages.add("⚠️ ${needed.name}: Short by $shortfallDisplay")
                }
            }

            if (shortages.isNotEmpty()) {
                log.add("Cannot cook ${recipe.name}!")
                log.addAll(shortages)
                log.add("Inventory was NOT updated.")
            } else {
                log.add("Cooking ${recipe.name}...")
                recipeRequirements.forEach { (needed, usableBatches, _) ->
                    var remainingToDeductBase = UnitConverter.toBaseUnit(needed.quantity, needed.unit)
                    var totalDeductedBase = 0.0
                    var batchesUsedCount = 0
                    
                    val sortedBatches = usableBatches.sortedWith(compareBy<IngredientEntity> { 
                        if (it.expirationDate == 0L) Long.MAX_VALUE else it.expirationDate 
                    })

                    for (batch in sortedBatches) {
                        if (remainingToDeductBase <= 0.005) break
                        
                        val batchQtyBase = UnitConverter.toBaseUnit(batch.quantity, batch.unit)
                        batchesUsedCount++
                        
                        if (batchQtyBase <= (remainingToDeductBase + 0.005)) {
                            totalDeductedBase += batchQtyBase
                            remainingToDeductBase -= batchQtyBase
                            dao.deleteIngredient(batch)
                        } else {
                            totalDeductedBase += remainingToDeductBase
                            val newQtyBase = batchQtyBase - remainingToDeductBase
                            val newQtyOriginalUnit = UnitConverter.fromBaseUnit(newQtyBase, batch.unit)
                            dao.insertOrUpdateIngredient(batch.copy(quantity = newQtyOriginalUnit))
                            remainingToDeductBase = 0.0
                        }
                    }
                    
                    val batchSuffix = if (batchesUsedCount > 1) " (from $batchesUsedCount batches)" else ""
                    log.add("✅ ${needed.name}: Used ${UnitConverter.formatDisplay(UnitConverter.fromBaseUnit(totalDeductedBase, needed.unit), needed.unit)}$batchSuffix")
                }
                log.add("Enjoy your meal! Inventory updated.")
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
