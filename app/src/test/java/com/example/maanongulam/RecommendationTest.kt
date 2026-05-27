package com.example.maanongulam

import org.junit.Assert.*
import org.junit.Test

class RecommendationTest {

    @Test
    fun testCorrectRanking() {
        // Mock timestamps: Tomorrow (urgent), Week (medium), Month (low)
        val currentTime = System.currentTimeMillis()
        val tomorrow = currentTime + 86400000L
        val nextWeek = currentTime + 86400000L * 7
        val nextMonth = currentTime + 86400000L * 30

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
        val currentTime = System.currentTimeMillis()
        // 1 day remaining -> daysRemaining = 1.0
        // Value = 1.0 / (1.0 + 1.0) = 0.5
        val tomorrow = currentTime + (24 * 60 * 60 * 1000L)
        
        // Inventory has 100g of Milk
        val inventory = listOf(Ingredient("Milk", 100.0, "g", tomorrow))
        
        // Recipe only needs 50g (50% of available inventory)
        val recipe = Recipe("Small Milk Dish", listOf(Ingredient("Milk", 50.0, "g", 0L)))

        val recommendations = RecipeRecommendationEngine.recommendTopRecipes(inventory, listOf(recipe))
        val score = recommendations[0].urgencyScore

        // Expected score: (UrgencyPerUnit * InventoryWeight) * (Capacity / InventoryWeight)
        // UrgencyPerUnit = 1.0 / (1.0 + 1.0) = 0.5
        // Capacity = 50.0
        // Score = 0.5 * 50.0 = 25.0
        assertEquals("Score should be UrgencyPerUnit * quantity used", 25.0, score, 0.05)
    }

    @Test
    fun testInsufficientQuantityFlag() {
        val inventory = listOf(Ingredient("Egg", 1.0, "pcs", 0L)) // 1 Egg (non-perishable for test)
        
        // Recipe needs 2 Eggs
        val recipe = Recipe("Omelet", listOf(Ingredient("Egg", 2.0, "pcs", 0L)))

        val recommendations = RecipeRecommendationEngine.recommendTopRecipes(inventory, listOf(recipe))
        
        assertTrue("Recipe should be flagged as insufficient", recommendations[0].isInsufficient)
        
        // Test sufficient case
        val sufficientInventory = listOf(Ingredient("Egg", 3.0, "pcs", 0L))
        val recommendationsSufficient = RecipeRecommendationEngine.recommendTopRecipes(sufficientInventory, listOf(recipe))
        
        assertFalse("Recipe should NOT be flagged as insufficient", recommendationsSufficient[0].isInsufficient)
    }
}
