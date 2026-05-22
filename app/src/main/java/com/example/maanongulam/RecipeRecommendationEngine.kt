package com.example.maanongulam

/**
 * Data class representing a recipe recommendation result.
 */
data class RecommendedRecipe(
    val recipe: Recipe,
    val urgencyScore: Double,
    val isInsufficient: Boolean
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
        return recipes.map { recipe ->
            // 1. Check for ingredient sufficiency (Flagging)
            val isInsufficient = checkInsufficiency(inventory, recipe)

            // 2. Prepare inventory items matching the recipe, normalized to base units (g/ml)
            val matchingInventory = inventory
                .filter { invItem -> recipe.ingredients.any { it.name.equals(invItem.name, ignoreCase = true) } }
                .map { it.copy(quantity = UnitConverter.toBaseUnit(it.quantity, it.unit), unit = "g/ml") }

            // 3. Calculate Capacity (Total metric quantity needed by the recipe)
            val capacity = recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }

            // 4. Calculate Urgency Score via Fractional Knapsack
            val score = RecommendationEngine.fractionalKnapsack(matchingInventory, capacity)

            RecommendedRecipe(recipe, score, isInsufficient)
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
