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

    private fun String.normalize() = this.replace("\\s".toRegex(), "").lowercase()

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

        // Pre-group full inventory for spoiled checks (Step 2 & 3)
        val fullInventoryMap = inventory.groupBy { it.name.normalize() }

        // Pre-filter and group usable inventory once (Step 1 & 4)
        val usableInventoryMap = inventory
            .filter { it.expirationDate == 0L || (it.expirationDate + expirationBuffer) >= currentTime }
            .groupBy { it.name.normalize() }

        return recipes.map { recipe ->
            val recipeIngNamesNorm = recipe.ingredients.map { it.name.normalize() }
            
            // 1. Get missing ingredients (considering only usable items)
            val missing = getMissingIngredients(usableInventoryMap, recipe)
            val isInsufficient = missing.isNotEmpty()

            // 2. Identify matching inventory items (including spoiled for warnings)
            val matchingInvItems = recipeIngNamesNorm.flatMap { name -> fullInventoryMap[name] ?: emptyList() }

            // 3. Safety Check: Does it use spoiled ingredients?
            val spoiledInRecipe = matchingInvItems.filter { 
                it.expirationDate > 0 && (it.expirationDate + expirationBuffer) < currentTime && it.quantity > 0 
            }
            val hasSpoiled = spoiledInRecipe.isNotEmpty()

            // 4. Prepare "Capped" inventory items for Knapsack (Multi-batch Virtual Summing)
            val cappedInvForScore = mutableListOf<Ingredient>()
            recipe.ingredients.forEach { required ->
                val reqNameNorm = required.name.normalize()
                val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
                
                val batches = (usableInventoryMap[reqNameNorm] ?: emptyList())
                    .sortedWith(compareBy<Ingredient> { 
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
     */
    private fun getMissingIngredients(usableInventoryMap: Map<String, List<Ingredient>>, recipe: Recipe): List<Ingredient> {
        val missing = mutableListOf<Ingredient>()
        
        for (required in recipe.ingredients) {
            val reqNameNorm = required.name.normalize()
            
            val totalAvailableBase = (usableInventoryMap[reqNameNorm] ?: emptyList())
                .sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
            
            val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
            
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
