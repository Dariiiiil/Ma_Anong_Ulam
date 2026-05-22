package com.example.maanongulam

/**
 * Data class representing a recipe recommendation result.
 */
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean,
    val reasons: List<String> = emptyList()
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

            // 2. Prepare inventory items matching the recipe
            val recipeIngNames = recipe.ingredients.map { it.name.lowercase() }
            val matchingInvItems = inventory.filter { it.name.lowercase() in recipeIngNames }

            val normalizedMatchingInv = matchingInvItems.map { 
                it.copy(quantity = UnitConverter.toBaseUnit(it.quantity, it.unit), unit = "g/ml") 
            }

            // 3. Calculate Capacity (Total metric quantity needed by the recipe)
            val capacity = recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }

            // 4. Calculate Urgency Score via Fractional Knapsack
            val score = RecommendationEngine.fractionalKnapsack(normalizedMatchingInv, capacity)

            // 5. Generate reasons
            val reasons = mutableListOf<String>()
            
            // Sort matching items by most urgent (closest to expiry)
            val urgentItems = matchingInvItems
                .filter { it.expirationDate > 0 && it.quantity > 0 }
                .sortedBy { it.expirationDate }
                .take(2)

            urgentItems.forEach { item ->
                val daysLeft = ((item.expirationDate - currentTime).toDouble() / (1000 * 60 * 60 * 24)).toInt()
                val dayWord = if (daysLeft == 1) "day" else "days"
                
                when {
                    daysLeft < 0 -> reasons.add("🕒 ${item.name} is expired!")
                    daysLeft == 0 -> reasons.add("🕒 ${item.name} expires today!")
                    daysLeft <= 3 -> reasons.add("🕒 ${item.name} expires in $daysLeft $dayWord")
                    else -> reasons.add("📦 Uses your ${item.name}")
                }
            }

            if (reasons.isEmpty()) {
                if (!isInsufficient) {
                    reasons.add("✅ Ready to cook: All items in stock")
                } else {
                    reasons.add("📝 Stock up soon for this recipe")
                }
            }

            RecommendedRecipe(recipe, score, isInsufficient, reasons)
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
