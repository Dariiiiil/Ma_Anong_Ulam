package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * --- RECOMMENDATION VIEW MODEL ---
 * Responsibility: Manages recipe logic, cooking logs, and recommendation triggers.
 */
class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.maAnongUlamDao()

    // Observes all recipes from the database
    val allRecipes: StateFlow<List<RecipeEntity>> = dao.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observes all ingredients in the inventory
    val allIngredients: StateFlow<List<IngredientEntity>> = dao.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Observes the history of cooked meals
    val allCookingLogs: StateFlow<List<CookingLogEntity>> = dao.getAllCookingLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * COMBINED RECOMMENDATIONS FLOW
     * This reacts whenever ingredients or recipes change.
     * It maps the database entities to domain models and runs the Recommendation Engine.
     */
    val recommendations: StateFlow<List<RecommendedRecipe>> = combine(
        allIngredients.map { entities -> entities.map { it.toDomainModel() } },
        allRecipes.map { entities -> entities.map { it.toDomainModel() } }
    ) { inventory, recipes ->
        if (inventory.isEmpty() || recipes.isEmpty()) emptyList<RecommendedRecipe>()
        else {
            // Run recommendation logic in the background thread (Default dispatcher)
            withContext(Dispatchers.Default) {
                RecipeRecommendationEngine.recommendTopRecipes(inventory, recipes)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States for reporting and handling user flow
    private val _cookingLog = MutableStateFlow<List<String>?>(null)
    val cookingLog: StateFlow<List<String>?> = _cookingLog.asStateFlow()

    private val _pendingShoppingDuplicates = MutableStateFlow<List<Pair<ShoppingItem, Ingredient>>?>(null)
    val pendingShoppingDuplicates: StateFlow<List<Pair<ShoppingItem, Ingredient>>?> = _pendingShoppingDuplicates.asStateFlow()

    private val _lastCookingDeductions = MutableStateFlow<List<Pair<IngredientEntity, Double>>?>(null)
    val lastCookingDeductions: StateFlow<List<Pair<IngredientEntity, Double>>?> = _lastCookingDeductions.asStateFlow()

    private val _lastFailedRecipe = MutableStateFlow<Recipe?>(null)
    val lastFailedRecipe: StateFlow<Recipe?> = _lastFailedRecipe.asStateFlow()

    /**
     * COOK RECIPE LOGIC
     * 1. Calculates shortages for the requested recipe.
     * 2. If 'force' is true, it proceeds even with missing items.
     * 3. Uses a Database Transaction to ensure inventory is updated accurately.
     */
    fun cookRecipe(recipe: Recipe, force: Boolean = false) {
        viewModelScope.launch {
            val log = mutableListOf<String>()
            val shortages = mutableListOf<String>()
            val ingredientsToDeduct = mutableMapOf<String, List<Pair<IngredientEntity, Double>>>()

            // Analyze inventory for each required ingredient
            recipe.ingredients.forEach { needed ->
                val matchingBatches = dao.getIngredientsByName(needed.name.trim())
                val totalAvailableBase = matchingBatches.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                val neededBase = UnitConverter.toBaseUnit(needed.quantity, needed.unit)

                // Detect shortages (using a small float epsilon for accuracy)
                if (totalAvailableBase < (neededBase - 0.001)) {
                    val shortfallBase = neededBase - totalAvailableBase
                    val shortfallDisplay = UnitConverter.formatDisplay(UnitConverter.fromBaseUnit(shortfallBase, needed.unit), needed.unit)
                    shortages.add("❌ ${needed.name}: Short by $shortfallDisplay")
                }
                
                // Prioritize batches by Expiry Date (FIFO logic)
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

            // Check if we can proceed
            if (shortages.isNotEmpty() && !force) {
                log.add("Cannot cook ${recipe.name} fully!")
                log.addAll(shortages)
                log.add("\nWould you like to cook anyway using available stock?")
                _lastFailedRecipe.value = recipe
            } else {
                log.add(if (force) "Cooking ${recipe.name} with missing items..." else "Cooking ${recipe.name}...")
                val allDeductions = mutableListOf<Pair<IngredientEntity, Double>>()
                
                // Perform the actual database deductions
                database.withTransaction {
                    ingredientsToDeduct.forEach { (name, deductions) ->
                        var totalUsedInNeededUnit = 0.0
                        val neededUnit = recipe.ingredients.first { it.name.equals(name, true) }.unit
                        deductions.forEach { (entity, amountToDeduct) ->
                            val newQty = (entity.quantity - amountToDeduct).coerceAtLeast(0.0)
                            if (newQty <= 0.001) dao.deleteIngredient(entity)
                            else dao.insertOrUpdateIngredient(entity.copy(quantity = newQty))
                            
                            // Convert used amount for the summary log
                            totalUsedInNeededUnit += UnitConverter.fromBaseUnit(UnitConverter.toBaseUnit(amountToDeduct, entity.unit), neededUnit)
                            allDeductions.add(entity to amountToDeduct)
                        }
                        log.add("✅ $name: Used ${UnitConverter.formatDisplay(totalUsedInNeededUnit, neededUnit)}")
                    }
                    // Record the session in the log
                    dao.insertCookingLog(CookingLogEntity(recipeName = recipe.name, timestamp = System.currentTimeMillis()))
                }
                _lastCookingDeductions.value = allDeductions
                _lastFailedRecipe.value = null
                log.add("\nInventory updated.")
            }
            _cookingLog.value = log
        }
    }

    /**
     * UNDO COOK FEATURE
     * Reverses the most recent inventory deduction and removes the history entry.
     */
    fun undoCook() {
        val deductions = _lastCookingDeductions.value ?: return
        viewModelScope.launch {
            database.withTransaction {
                deductions.forEach { (originalEntity, amount) ->
                    val current = dao.getIngredientById(originalEntity.id)
                    if (current != null) {
                        dao.insertOrUpdateIngredient(current.copy(quantity = current.quantity + amount))
                    } else {
                        // Re-create the batch if it was deleted
                        dao.insertOrUpdateIngredient(originalEntity.copy(quantity = amount))
                    }
                }
                // Remove the last cooking log entry
                val lastLog = dao.getAllCookingLogsOnce().firstOrNull()
                if (lastLog != null) {
                    dao.deleteCookingLog(lastLog)
                }
            }
            _lastCookingDeductions.value = null
            _cookingLog.value = listOf("Undo successful! Ingredients restored.")
        }
    }

    fun clearCookingLog() { _cookingLog.value = null }

    fun addRecipe(name: String, ingredients: List<Ingredient>) {
        viewModelScope.launch {
            val normalizedIngredients = ingredients.map { it.copy(name = it.name.trim()) }
            dao.insertRecipe(RecipeEntity(name = name.trim(), ingredients = normalizedIngredients))
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

    /**
     * SHOPPING INTEGRATION
     * Merges missing recipe items into the shopping list.
     * Identifies duplicates and asks the user for confirmation.
     */
    fun addMissingToShoppingList(missing: List<Ingredient>, isManual: Boolean = true) {
        viewModelScope.launch {
            val mergedMissing = missing.groupBy { it.name.trim().lowercase() }.map { (_, items) ->
                val first = items.first()
                val totalBase = items.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                Ingredient(name = first.name.trim(), quantity = UnitConverter.fromBaseUnit(totalBase, first.unit), unit = first.unit, expirationDate = 0L)
            }
            val duplicates = mutableListOf<Pair<ShoppingItem, Ingredient>>()
            val newItems = mutableListOf<Ingredient>()
            mergedMissing.forEach { item ->
                val existing = dao.getShoppingItemByName(item.name)
                if (existing != null) { if (isManual) duplicates.add(existing to item) }
                else { newItems.add(item) }
            }
            newItems.forEach { item -> dao.insertShoppingItem(ShoppingItem(name = item.name, quantity = item.quantity, unit = item.unit)) }
            if (isManual && duplicates.isNotEmpty()) { _pendingShoppingDuplicates.value = duplicates }
            else { _pendingShoppingDuplicates.value = null }
        }
    }

    fun confirmShoppingDuplicates(merge: Boolean) {
        val pending = _pendingShoppingDuplicates.value ?: return
        viewModelScope.launch {
            if (merge) {
                pending.forEach { (existing, newItem) ->
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
            if (topRecommendation != null && topRecommendation.isInsufficient && !topRecommendation.hasSpoiledIngredients) {
                addMissingToShoppingList(topRecommendation.missingIngredients, isManual = false)
            }
        }
    }
}

/**
 * --- INGREDIENT VIEW MODEL ---
 * Responsibility: Handles adding, updating, and deleting inventory items.
 */
class IngredientViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    val ingredients: StateFlow<List<IngredientEntity>> = dao.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodDefinitions: StateFlow<List<FoodDefinitionEntity>> = dao.getAllFoodDefinitions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addIngredient(name: String, quantity: Double, unit: String, expirationDate: Long, category: String) {
        if (quantity <= 0.0) return
        viewModelScope.launch {
            dao.insertOrUpdateIngredient(IngredientEntity(name = name, quantity = quantity, unit = unit, expirationDate = expirationDate, category = category))
        }
    }

    fun addFoodDefinition(name: String, unitType: String, category: String, isImperishable: Boolean = false) {
        viewModelScope.launch {
            dao.insertFoodDefinition(FoodDefinitionEntity(name = name, unitType = unitType, category = category, isImperishable = isImperishable))
        }
    }

    fun deleteFoodDefinition(definition: FoodDefinitionEntity) {
        viewModelScope.launch {
            dao.deleteFoodDefinition(definition)
        }
    }

    fun updateFoodDefinition(definition: FoodDefinitionEntity) {
        viewModelScope.launch {
            dao.insertFoodDefinition(definition)
        }
    }

    fun updateIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            if (ingredient.quantity <= 0.0) {
                dao.deleteIngredient(ingredient)
            } else {
                dao.insertOrUpdateIngredient(ingredient)
            }
        }
    }

    /**
     * MOVE TO INVENTORY
     * Logic for transferring a bought shopping item into the pantry/fridge.
     */
    fun moveToInventory(item: ShoppingItem, foodDefinition: FoodDefinitionEntity?) {
        viewModelScope.launch {
            val expiry = if (foodDefinition?.isImperishable == true) 0L else {
                System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // Default 1 week shelf life
            }
            val category = foodDefinition?.category ?: "Others"
            
            // Check for existing matching batches to merge quantities
            val existing = dao.getIngredientByName(item.name)
            if (existing != null && existing.expirationDate == expiry) {
                val totalBase = UnitConverter.toBaseUnit(existing.quantity, existing.unit) + UnitConverter.toBaseUnit(item.quantity, item.unit)
                dao.insertOrUpdateIngredient(existing.copy(quantity = UnitConverter.fromBaseUnit(totalBase, existing.unit)))
            } else {
                dao.insertOrUpdateIngredient(IngredientEntity(
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    expirationDate = expiry,
                    category = category
                ))
            }
            dao.deleteShoppingItem(item)
        }
    }

    fun deleteIngredient(ingredient: IngredientEntity) { viewModelScope.launch { dao.deleteIngredient(ingredient) } }
    fun deleteAllIngredients() { viewModelScope.launch { dao.deleteAllIngredients() } }
}

/**
 * --- SHOPPING VIEW MODEL ---
 * Responsibility: Manages the shopping list state and checked items.
 */
class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    val shoppingItems: StateFlow<List<ShoppingItem>> = dao.getAllShoppingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            val existing = dao.getShoppingItemByName(name)
            if (existing != null) {
                val totalBase = UnitConverter.toBaseUnit(existing.quantity, existing.unit) + UnitConverter.toBaseUnit(quantity, unit)
                dao.insertShoppingItem(existing.copy(quantity = UnitConverter.fromBaseUnit(totalBase, existing.unit)))
            } else {
                dao.insertShoppingItem(ShoppingItem(name = name, quantity = quantity, unit = unit))
            }
        }
    }

    fun toggleChecked(item: ShoppingItem) { viewModelScope.launch { dao.insertShoppingItem(item.copy(isChecked = !item.isChecked)) } }
    fun updateItem(item: ShoppingItem) { viewModelScope.launch { dao.insertShoppingItem(item) } }
    fun deleteItem(item: ShoppingItem) { viewModelScope.launch { dao.deleteShoppingItem(item) } }
    fun clearCheckedItems() {
        viewModelScope.launch {
            shoppingItems.value.filter { it.isChecked }.forEach { dao.deleteShoppingItem(it) }
        }
    }
}

// --- Legacy/Simple Recipe Handler ---
class RecipeViewModel : ViewModel() {
    private val _inventory = MutableStateFlow<List<Ingredient>>(emptyList())
    val inventory: StateFlow<List<Ingredient>> = _inventory.asStateFlow()
    private val _allRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val _recommendations = MutableStateFlow<List<RecommendedRecipe>>(emptyList())
    val recommendations: StateFlow<List<RecommendedRecipe>> = _recommendations.asStateFlow()

    fun updateInventory(newInventory: List<Ingredient>) { _inventory.value = newInventory; refreshRecommendations() }
    fun updateRecipes(newRecipes: List<Recipe>) { _allRecipes.value = newRecipes; refreshRecommendations() }
    private fun refreshRecommendations() {
        if (_inventory.value.isNotEmpty() && _allRecipes.value.isNotEmpty()) {
            _recommendations.value = RecipeRecommendationEngine.recommendTopRecipes(_inventory.value, _allRecipes.value)
        }
    }
}
