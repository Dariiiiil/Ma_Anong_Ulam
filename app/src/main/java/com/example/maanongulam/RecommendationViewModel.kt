package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.maAnongUlamDao()

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
        if (inventory.isEmpty() || recipes.isEmpty()) emptyList<RecommendedRecipe>()
        else {
            withContext(Dispatchers.Default) {
                RecipeRecommendationEngine.recommendTopRecipes(inventory, recipes)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cookingLog = MutableStateFlow<List<String>?>(null)
    val cookingLog: StateFlow<List<String>?> = _cookingLog.asStateFlow()

    private val _pendingShoppingDuplicates = MutableStateFlow<List<Pair<ShoppingItem, Ingredient>>?>(null)
    val pendingShoppingDuplicates: StateFlow<List<Pair<ShoppingItem, Ingredient>>?> = _pendingShoppingDuplicates.asStateFlow()

    fun cookRecipe(recipe: Recipe) {
        viewModelScope.launch {
            val log = mutableListOf<String>()
            val shortages = mutableListOf<String>()
            val ingredientsToDeduct = mutableMapOf<String, List<Pair<IngredientEntity, Double>>>()

            recipe.ingredients.forEach { needed ->
                val matchingBatches = dao.getIngredientsByName(needed.name.trim())
                val totalAvailableBase = matchingBatches.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                val neededBase = UnitConverter.toBaseUnit(needed.quantity, needed.unit)

                if (totalAvailableBase < neededBase) {
                    val shortfallBase = neededBase - totalAvailableBase
                    val shortfallDisplay = UnitConverter.formatDisplay(UnitConverter.fromBaseUnit(shortfallBase, needed.unit), needed.unit)
                    shortages.add("❌ ${needed.name}: Short by $shortfallDisplay")
                } else {
                    val sortedBatches = matchingBatches.sortedWith(compareBy<IngredientEntity> { it.expirationDate == 0L }.thenBy { it.expirationDate })
                    var remainingToDeductBase = neededBase
                    val plannedDeductions = mutableListOf<Pair<IngredientEntity, Double>>()
                    for (batch in sortedBatches) {
                        if (remainingToDeductBase <= 0) break
                        val batchBase = UnitConverter.toBaseUnit(batch.quantity, batch.unit)
                        val deductFromThisBatchBase = minOf(batchBase, remainingToDeductBase)
                        val deductInOriginalUnit = UnitConverter.fromBaseUnit(deductFromThisBatchBase, batch.unit)
                        plannedDeductions.add(batch to deductInOriginalUnit)
                        remainingToDeductBase -= deductFromThisBatchBase
                    }
                    ingredientsToDeduct[needed.name] = plannedDeductions
                }
            }

            if (shortages.isNotEmpty()) {
                log.add("Cannot cook ${recipe.name}!")
                log.addAll(shortages)
                log.add("\nInventory was NOT updated.")
            } else {
                log.add("Cooking ${recipe.name}...")
                database.withTransaction {
                    ingredientsToDeduct.forEach { (name, deductions) ->
                        var totalUsedInNeededUnit = 0.0
                        val neededUnit = recipe.ingredients.first { it.name.equals(name, true) }.unit
                        deductions.forEach { (entity, amountToDeduct) ->
                            val newQty = (entity.quantity - amountToDeduct).coerceAtLeast(0.0)
                            if (newQty <= 0.001) dao.deleteIngredient(entity)
                            else dao.insertOrUpdateIngredient(entity.copy(quantity = newQty))
                            totalUsedInNeededUnit += UnitConverter.fromBaseUnit(UnitConverter.toBaseUnit(amountToDeduct, entity.unit), neededUnit)
                        }
                        log.add("✅ $name: Used ${UnitConverter.formatDisplay(totalUsedInNeededUnit, neededUnit)}")
                    }
                    dao.insertCookingLog(CookingLogEntity(recipeName = recipe.name, timestamp = System.currentTimeMillis()))
                }
                log.add("\nEnjoy your meal! Inventory updated.")
            }
            _cookingLog.value = log
        }
    }

    fun clearCookingLog() { _cookingLog.value = null }

    fun addRecipe(name: String, ingredients: List<Ingredient>) {
        viewModelScope.launch {
            val normalizedIngredients = ingredients.map { it.copy(name = it.name.trim()) }
            dao.insertRecipe(RecipeEntity(name = name.trim(), ingredients = normalizedIngredients))
        }
    }

    fun addIngredientIfMissing(name: String, unit: String, expirationDate: Long, category: String) {
        viewModelScope.launch {
            val normalizedName = name.trim()
            if (dao.getIngredientByName(normalizedName) == null) {
                dao.insertOrUpdateIngredient(IngredientEntity(name = normalizedName, quantity = 0.0, unit = unit, expirationDate = expirationDate, category = category))
            }
        }
    }

    fun updateRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            val normalizedRecipe = recipe.copy(name = recipe.name.trim(), ingredients = recipe.ingredients.map { it.copy(name = it.name.trim()) })
            dao.insertRecipe(normalizedRecipe)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) { viewModelScope.launch { dao.deleteRecipe(recipe) } }
    fun deleteAllRecipes() { viewModelScope.launch { dao.deleteAllRecipes() } }
    fun deleteAllCookingLogs() { viewModelScope.launch { dao.deleteAllCookingLogs() } }

    fun addMissingToShoppingList(missing: List<Ingredient>, isManual: Boolean = true) {
        viewModelScope.launch {
            // 1. Group missing items by name to avoid internal duplicates
            val mergedMissing = missing.groupBy { it.name.trim().lowercase() }.map { (lowName, items) ->
                val first = items.first()
                val totalBase = items.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                Ingredient(
                    name = first.name.trim(),
                    quantity = UnitConverter.fromBaseUnit(totalBase, first.unit),
                    unit = first.unit,
                    expirationDate = 0L
                )
            }

            val duplicates = mutableListOf<Pair<ShoppingItem, Ingredient>>()
            val newItems = mutableListOf<Ingredient>()
            
            mergedMissing.forEach { item ->
                val existing = dao.getShoppingItemByName(item.name)
                if (existing != null) {
                    // Only track as duplicate if it's a manual request
                    if (isManual) {
                        duplicates.add(existing to item)
                    }
                } else {
                    newItems.add(item)
                }
            }
            
            // 2. Add truly new items to the database
            newItems.forEach { item ->
                dao.insertShoppingItem(ShoppingItem(name = item.name, quantity = item.quantity, unit = item.unit))
            }
            
            // 3. Show dialog ONLY for manual actions and ONLY if there are duplicates
            if (isManual && duplicates.isNotEmpty()) {
                _pendingShoppingDuplicates.value = duplicates
            } else {
                // Ensure dialog is null if there's nothing to show
                _pendingShoppingDuplicates.value = null
            }
        }
    }

    fun confirmShoppingDuplicates(merge: Boolean) {
        val pending = _pendingShoppingDuplicates.value ?: return
        viewModelScope.launch {
            if (merge) {
                pending.forEach { (existing, newItem) ->
                    // Normalize units before merging if possible
                    val totalBase = UnitConverter.toBaseUnit(existing.quantity, existing.unit) + UnitConverter.toBaseUnit(newItem.quantity, newItem.unit)
                    dao.insertShoppingItem(existing.copy(quantity = UnitConverter.fromBaseUnit(totalBase, existing.unit)))
                }
            }
            _pendingShoppingDuplicates.value = null
        }
    }

    fun dismissShoppingDuplicates() { _pendingShoppingDuplicates.value = null }

    fun runAutoRestockCheck() {
        viewModelScope.launch {
            val topRecommendation = recommendations.value.firstOrNull()
            // Auto-restock only for the "Ulam of the Day" if it's missing something
            if (topRecommendation != null && topRecommendation.isInsufficient && !topRecommendation.hasSpoiledIngredients) {
                addMissingToShoppingList(topRecommendation.missingIngredients, isManual = false)
            }
        }
    }
}
