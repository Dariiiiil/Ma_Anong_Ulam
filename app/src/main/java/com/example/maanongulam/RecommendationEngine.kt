package com.example.maanongulam

/**
 * Engine to handle recommendation logic for food spoilage tracking.
 */
object RecommendationEngine {

    /**
     * Implements the Fractional Knapsack Algorithm to prioritize ingredients based on urgency.
     *
     * Urgency Score (Value): Inversely proportional to the expiration date.
     * Weight: The quantity of the ingredient.
     * Density: Urgency Score / Weight.
     *
     * @param ingredientsList List of [Ingredient] available.
     * @param capacity The maximum capacity (e.g., total volume or weight) for the recommendation.
     * @return The total urgency value of the selected fractional items.
     */
    fun fractionalKnapsack(ingredientsList: List<Ingredient>, capacity: Double): Double {
        // 1. Calculate the value-to-weight ratio (Urgency Density)
        // item.ratio = item.value / item.weight
        val itemsWithDensity = ingredientsList.map { ingredient ->
            // Mathematically inversely proportional to the expiration date
            // We use a double to maintain precision for the ratio
            val value = if (ingredient.expirationDate > 0) {
                1.0 / ingredient.expirationDate.toDouble()
            } else {
                Double.MAX_VALUE // Immediate urgency if expired or invalid
            }
            
            val weight = ingredient.quantity
            val ratio = if (weight > 0) value / weight else 0.0
            
            KnapsackItem(value, weight, ratio)
        }

        // 2. Sort ingredients based on ratio in descending order
        val sortedItems = itemsWithDensity.sortedByDescending { it.ratio }
        
        var totalValue = 0.0
        var currentWeight = 0.0

        // 3. Iterate through sorted items
        for (item in sortedItems) {
            if (currentWeight + item.weight <= capacity) {
                currentWeight += item.weight
                totalValue += item.value
            } else {
                val remaining = capacity - currentWeight
                if (item.weight > 0) {
                    totalValue += item.value * (remaining / item.weight)
                }
                currentWeight += remaining
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
