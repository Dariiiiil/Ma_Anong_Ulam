package com.example.maanongulam

import org.junit.Assert.*
import org.junit.Test

class RecommendationTest {

    @Test
    fun testCorrectRanking() {
        // Mock timestamps: Tomorrow (smaller), Week (medium), Month (large)
        val tomorrow = 1000L
        val nextWeek = 7000L
        val nextMonth = 30000L

        val inventory = listOf(
            Ingredient("Pork", 1000.0, "g", tomorrow),
            Ingredient("Chicken", 1000.0, "g", nextWeek),
            Ingredient("Beef", 1000.0, "g", nextMonth)
        )

        val recipeUrgent = Recipe("Urgent Pork", listOf(Ingredient("Pork", 500.0, "g", 0L)))
        val recipeMedium = Recipe("Medium Chicken", listOf(Ingredient("Chicken", 500.0, "g", 0L)))
        val recipeLow = Recipe("Low Beef", listOf(Ingredient("Beef", 500.0, "g", 0L)))

        val recipes = listOf(recipeMedium, recipeUrgent, recipeLow)
        
        val recommendations = RecipeRecommendationEngine.recommendTopRecipes(inventory, recipes)

        assertEquals("Top recommendation should be the one with the soonest expiration", "Urgent Pork", recommendations[0].recipe.name)
        assertEquals("Second recommendation should be medium urgency", "Medium Chicken", recommendations[1].recipe.name)
        assertEquals("Third recommendation should be lowest urgency", "Low Beef", recommendations[2].recipe.name)
    }

    @Test
    fun testFractionalLogic() {
        // Value = 1 / expirationDate. For 1000L, Value = 0.001
        val expiration = 1000L
        val expectedFullValue = 1.0 / expiration.toDouble()

        // Inventory has 100g of Milk
        val inventory = listOf(Ingredient("Milk", 100.0, "g", expiration))
        
        // Recipe only needs 50g (50% of available inventory)
        // Capacity = 50.0
        val recipe = Recipe("Small Milk Dish", listOf(Ingredient("Milk", 50.0, "g", 0L)))

        val recommendations = RecipeRecommendationEngine.recommendTopRecipes(inventory, listOf(recipe))
        val score = recommendations[0].urgencyScore

        // Expected score: Value * (Capacity / InventoryWeight) = 0.001 * (50 / 100) = 0.0005
        assertEquals("Score should be exactly 50% of the total ingredient value", expectedFullValue * 0.5, score, 0.000001)
    }

    @Test
    fun testInsufficientQuantityFlag() {
        val inventory = listOf(Ingredient("Egg", 1.0, "pcs", 1000L)) // 1 Egg
        
        // Recipe needs 2 Eggs
        val recipe = Recipe("Omelet", listOf(Ingredient("Egg", 2.0, "pcs", 0L)))

        val recommendations = RecipeRecommendationEngine.recommendTopRecipes(inventory, listOf(recipe))
        
        assertTrue("Recipe should be flagged as insufficient", recommendations[0].isInsufficient)
        
        // Test sufficient case
        val sufficientInventory = listOf(Ingredient("Egg", 3.0, "pcs", 1000L))
        val recommendationsSufficient = RecipeRecommendationEngine.recommendTopRecipes(sufficientInventory, listOf(recipe))
        
        assertFalse("Recipe should NOT be flagged as insufficient", recommendationsSufficient[0].isInsufficient)
    }
}
