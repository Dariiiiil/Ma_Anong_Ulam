package com.example.maanongulam

/**
 * Engine to handle recommendation logic for food spoilage tracking.
 */
object RecommendationEngine {

    /**
     * Implements the Fractional Knapsack Algorithm to prioritize ingredients based on urgency.
     *
     * Urgency Score (Value): Higher for items closer to expiration.
     * Weight: The quantity of the ingredient.
     * Ratio: Value / Weight.
     *
     * @param ingredientsList List of [Ingredient] available.
     * @param capacity The maximum capacity (e.g., total volume or weight) for the recommendation.
     * @return The total urgency value of the selected fractional items.
     */
    fun fractionalKnapsack(ingredientsList: List<Ingredient>, capacity: Double): Double {
        val currentTime = System.currentTimeMillis()
        
        // 1. Calculate the value-to-weight ratio (Urgency Density)
        val itemsWithDensity = ingredientsList.map { ingredient ->
            // Calculate urgency based on time remaining until expiration
            val remainingMs = ingredient.expirationDate - currentTime
            
            val value = if (ingredient.expirationDate > 0) {
                val daysRemaining = remainingMs.toDouble() / (1000 * 60 * 60 * 24)
                if (daysRemaining <= 0) {
                    10.0 // Already expired or expires today: High urgency
                } else {
                    // Urgency decreases as days remaining increases.
                    // e.g., 1 day left -> 0.5, 9 days left -> 0.1
                    (1.0 / (daysRemaining + 1.0)) + 0.02 // Add base value for "having it"
                }
            } else {
                0.02 // Non-perishables contribute a base "availability" score
            }
            
            val weight = ingredient.quantity
            // Ratio determines priority in the knapsack
            val ratio = if (weight > 0) value / weight else 0.0
            
            KnapsackItem(value, weight, ratio)
        }

        // 2. Sort ingredients based on ratio in descending order
        val sortedItems = itemsWithDensity.sortedByDescending { it.ratio }
        
        var totalValue = 0.0
        var currentWeight = 0.0

        // 3. Iterate through sorted items to fill the "recipe capacity"
        for (item in sortedItems) {
            if (currentWeight + item.weight <= capacity) {
                currentWeight += item.weight
                totalValue += item.value
            } else {
                val remaining = capacity - currentWeight
                if (item.weight > 0) {
                    totalValue += item.value * (remaining / item.weight)
                }
                break
            }
        }

        return totalValue
    }

    /**
     * Internal helper to store knapsack calculation data.
     */
    private data class KnapsackItem(
        val value: Double,
        val weight: Double,
        val ratio: Double
    )
}
