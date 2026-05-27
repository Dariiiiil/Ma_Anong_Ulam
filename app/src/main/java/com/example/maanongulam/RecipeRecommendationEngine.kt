package com.example.maanongulam

/**
 * Data class representing a recipe recommendation result.
 */
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean,
    val reasons: List<String> = emptyList(),
    val hasSpoiledIngredients: Boolean = false,
    val missingIngredients: List<Ingredient> = emptyList()
)

/**
 * Engine to rank recipes based on inventory urgency using the Fractional Knapsack algorithm.
 */
object RecipeRecommendationEngine {

    /**
     * Ranks all recipes and returns the top 3 recommendations.
     *
     * @param inventory Current list of ingredients in inventory.
     * @param recipes List of all available recipes.
     * @return Top 3 [RecommendedRecipe] objects sorted by urgency.
     */
    fun recommendTopRecipes(inventory: List<Ingredient>, recipes: List<Recipe>): List<RecommendedRecipe> {
        val currentTime = System.currentTimeMillis()
        val expirationBuffer = 60000L // 1 minute buffer

        // Pre-filter usable inventory once
        val usableInventory = inventory.filter {
            it.expirationDate == 0L || (it.expirationDate + expirationBuffer) >= currentTime
        }

        return recipes.map { recipe ->
            // 1. Get missing ingredients (considering only usable items)
            val missing = getMissingIngredients(usableInventory, recipe)
            val isInsufficient = missing.isNotEmpty()

            // 2. Identify matching inventory items (including spoiled for warnings)
            val recipeIngNamesNorm = recipe.ingredients.map { it.name.replace("\\s".toRegex(), "").lowercase() }
            val matchingInvItems = inventory.filter { it.name.replace("\\s".toRegex(), "").lowercase() in recipeIngNamesNorm }

            // 3. Safety Check: Does it use spoiled ingredients?
            val spoiledInRecipe = matchingInvItems.filter { 
                it.expirationDate > 0 && (it.expirationDate + expirationBuffer) < currentTime && it.quantity > 0 
            }
            val hasSpoiled = spoiledInRecipe.isNotEmpty()

            // 4. Prepare "Capped" inventory items for Knapsack (Multi-batch Virtual Summing)
            // This prevents an overabundant ingredient from "filling capacity" for a missing ingredient.
            val cappedInvForScore = mutableListOf<Ingredient>()
            recipe.ingredients.forEach { required ->
                val reqNameNorm = required.name.replace("\\s".toRegex(), "").lowercase()
                val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
                
                val batches = usableInventory.filter { 
                    it.name.replace("\\s".toRegex(), "").lowercase() == reqNameNorm 
                }.sortedWith(compareBy<Ingredient> { 
                    if (it.expirationDate == 0L) Long.MAX_VALUE else it.expirationDate 
                })

                var takenBase = 0.0
                for (batch in batches) {
                    val batchBase = UnitConverter.toBaseUnit(batch.quantity, batch.unit)
                    val remainingNeeded = (requiredBase - takenBase).coerceAtLeast(0.0)
                    if (remainingNeeded <= 0) break
                    
                    val toTake = minOf(batchBase, remainingNeeded)
                    if (toTake > 0) {
                        cappedInvForScore.add(batch.copy(
                            quantity = UnitConverter.fromBaseUnit(toTake, batch.unit)
                        ))
                        takenBase += toTake
                    }
                }
            }

            val normalizedMatchingInv = cappedInvForScore.map { 
                it.copy(quantity = UnitConverter.toBaseUnit(it.quantity, it.unit), unit = "g/ml") 
            }

            // 5. Calculate Capacity (Total metric quantity needed by the recipe)
            val capacity = recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }

            // 6. Calculate Urgency Score via Fractional Knapsack
            val score = if (hasSpoiled) 0.0 else RecommendationEngine.fractionalKnapsack(normalizedMatchingInv, capacity)

            // 7. Generate reasons
            val reasons = mutableListOf<String>()
            
            if (hasSpoiled) {
                reasons.add("💀 Contains spoiled: ${spoiledInRecipe.joinToString { it.name }}")
            }

            // Sort matching items by most urgent
            val urgentItems = cappedInvForScore
                .filter { it.expirationDate > 0 && it.quantity > 0 }
                .sortedBy { it.expirationDate }
                .take(2)

            urgentItems.forEach { item ->
                val daysLeft = ((item.expirationDate - currentTime).toDouble() / (1000 * 60 * 60 * 24)).toInt()
                val dayWord = if (daysLeft == 1) "day" else "days"
                
                when {
                    daysLeft == 0 -> reasons.add("🕒 ${item.name} expires today!")
                    daysLeft <= 3 -> reasons.add("🕒 ${item.name} expires in $daysLeft $dayWord")
                    else -> reasons.add("📦 Uses your ${item.name}")
                }
            }

            if (reasons.isEmpty()) {
                if (missing.isEmpty()) {
                    reasons.add("✅ Ready to cook: All items in stock")
                } else {
                    missing.forEach { item ->
                        val missingDisplay = UnitConverter.formatDisplay(item.quantity, item.unit)
                        reasons.add("⚠️ Need $missingDisplay more ${item.name}")
                    }
                }
            }

            RecommendedRecipe(recipe, score, isInsufficient, reasons, hasSpoiled, missing)
        }
        .sortedByDescending { it.urgencyScore }
    }

    /**
     * Identifies which ingredients are missing or insufficient and by how much.
     * Uses robust normalization and filters out spoiled items.
     */
    private fun getMissingIngredients(usableInventory: List<Ingredient>, recipe: Recipe): List<Ingredient> {
        val missing = mutableListOf<Ingredient>()
        
        for (required in recipe.ingredients) {
            val reqNameNorm = required.name.replace("\\s".toRegex(), "").lowercase()
            
            val totalAvailableBase = usableInventory
                .filter { it.name.replace("\\s".toRegex(), "").lowercase() == reqNameNorm }
                .sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
            
            val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
            
            // Using 0.01 epsilon to avoid floating point issues
            if (totalAvailableBase < (requiredBase - 0.01)) {
                val diffBase = requiredBase - totalAvailableBase
                missing.add(Ingredient(
                    name = required.name,
                    quantity = UnitConverter.fromBaseUnit(diffBase, required.unit),
                    unit = required.unit,
                    expirationDate = 0L
                ))
            }
        }
        return missing
    }
}
