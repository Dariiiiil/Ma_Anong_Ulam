package com.example.maanongulam

/**
 * Data class representing a recipe recommendation result.
 */
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean,
    val reasons: List<String> = emptyList(),
    val hasSpoiledIngredients: Boolean = false
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
        
        return recipes.map { recipe ->
            // 1. Check for ingredient sufficiency (Flagging)
            val isInsufficient = checkInsufficiency(inventory, recipe)

            // 2. Identify matching inventory items
            val recipeIngNames = recipe.ingredients.map { it.name.lowercase() }
            val matchingInvItems = inventory.filter { it.name.lowercase() in recipeIngNames }

            // 3. Safety Check: Does it use spoiled ingredients?
            // An item is spoiled if it has an expirationDate > 0 AND that date is in the past.
            val spoiledInRecipe = matchingInvItems.filter { it.expirationDate > 0 && it.expirationDate < currentTime && it.quantity > 0 }
            val hasSpoiled = spoiledInRecipe.isNotEmpty()

            // 4. Prepare inventory items for algorithm - FILTER OUT spoiled (Option 1)
            // Usable items are those with expirationDate == 0 (non-perishable) OR expirationDate >= currentTime (not yet spoiled)
            val usableInv = matchingInvItems.filter { it.expirationDate == 0L || it.expirationDate >= currentTime }
            val normalizedMatchingInv = usableInv.map { 
                it.copy(quantity = UnitConverter.toBaseUnit(it.quantity, it.unit), unit = "g/ml") 
            }

            // 5. Calculate Capacity (Total metric quantity needed by the recipe)
            val capacity = recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }

            // 6. Calculate Urgency Score via Fractional Knapsack (Spoiled items contribute 0 score now)
            val score = if (hasSpoiled) 0.0 else RecommendationEngine.fractionalKnapsack(normalizedMatchingInv, capacity)

            // 7. Generate reasons
            val reasons = mutableListOf<String>()
            
            if (hasSpoiled) {
                reasons.add("💀 Contains spoiled: ${spoiledInRecipe.joinToString { it.name }}")
            }

            // Sort matching items by most urgent (closest to expiry, but NOT spoiled)
            val urgentItems = usableInv
                .filter { it.expirationDate > 0 && it.quantity > 0 }
                .sortedBy { it.expirationDate }
                .take(2)

            urgentItems.forEach { item ->
                if (item.expirationDate > 0) {
                    val daysLeft = ((item.expirationDate - currentTime).toDouble() / (1000 * 60 * 60 * 24)).toInt()
                    val dayWord = if (daysLeft == 1) "day" else "days"
                    
                    when {
                        daysLeft == 0 -> reasons.add("🕒 ${item.name} expires today!")
                        daysLeft <= 3 -> reasons.add("🕒 ${item.name} expires in $daysLeft $dayWord")
                        else -> reasons.add("📦 Uses your ${item.name}")
                    }
                } else {
                    reasons.add("📦 Uses your ${item.name}")
                }
            }

            if (reasons.isEmpty()) {
                if (!isInsufficient) {
                    reasons.add("✅ Ready to cook: All items in stock")
                } else {
                    reasons.add("📝 Stock up soon for this recipe")
                }
            }

            RecommendedRecipe(recipe, score, isInsufficient, reasons, hasSpoiled)
        }
        .sortedByDescending { it.urgencyScore }
        .take(3)
    }

    /**
     * Checks if the total available quantity of each required ingredient in inventory
     * is less than the recipe's requirement.
     */
    private fun checkInsufficiency(inventory: List<Ingredient>, recipe: Recipe): Boolean {
        for (required in recipe.ingredients) {
            val totalAvailable = inventory
                .filter { it.name.equals(required.name, ignoreCase = true) }
                .sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
            
            if (totalAvailable < UnitConverter.toBaseUnit(required.quantity, required.unit)) {
                return true
            }
        }
        return false
    }
}
