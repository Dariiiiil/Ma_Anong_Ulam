package com.example.maanongulam

import java.util.Locale

// --- Unit Converter ---

object UnitConverter {
    fun toBaseUnit(quantity: Double, unit: String): Double {
        return when (unit.lowercase().trim()) {
            "kg", "kilogram", "kilograms", "kilo" -> quantity * 1000.0
            "l", "liter", "liters", "litre" -> quantity * 1000.0
            else -> quantity
        }
    }

    fun fromBaseUnit(baseQuantity: Double, targetUnit: String): Double {
        return when (targetUnit.lowercase().trim()) {
            "kg", "kilogram", "kilograms", "kilo" -> baseQuantity / 1000.0
            "l", "liter", "liters", "litre" -> baseQuantity / 1000.0
            else -> baseQuantity
        }
    }

    fun isLowStock(quantity: Double, unit: String, category: String = "Others"): Boolean {
        val base = toBaseUnit(quantity, unit)
        if (base <= 0) return false
        return when (category.lowercase().trim()) {
            "spices" -> base <= 6.0
            "grains" -> base <= 150.0
            "meat", "seafood" -> base <= 90.0
            "vegetables" -> base <= 60.0
            "dairy" -> base <= 60.0
            else -> base <= 30.0
        }
    }

    fun formatDisplay(quantity: Double, unit: String): String {
        val lowerUnit = unit.lowercase().trim()
        val base = toBaseUnit(quantity, unit)
        return when {
            (lowerUnit == "g" || lowerUnit == "kg" || lowerUnit == "gram" || lowerUnit == "grams") && base >= 1000.0 -> {
                val value = base / 1000.0
                val formattedValue = if (value == value.toInt().toDouble()) "${value.toInt()}" else String.format(Locale.getDefault(), "%.2f", value)
                "${formattedValue}kg"
            }
            (lowerUnit == "ml" || lowerUnit == "l" || lowerUnit == "liter" || lowerUnit == "liters") && base >= 1000.0 -> {
                val value = base / 1000.0
                val formattedValue = if (value == value.toInt().toDouble()) "${value.toInt()}" else String.format(Locale.getDefault(), "%.2f", value)
                "${formattedValue}L"
            }
            else -> {
                val formattedValue = if (quantity == quantity.toInt().toDouble()) "${quantity.toInt()}" else String.format(Locale.getDefault(), "%.2f", quantity)
                "$formattedValue$unit"
            }
        }
    }
}

// --- Recommendation Logic ---

object RecommendationEngine {
    fun fractionalKnapsack(ingredientsList: List<Ingredient>, capacity: Double): Double {
        val currentTime = System.currentTimeMillis()
        val itemsWithDensity = ingredientsList.map { ingredient ->
            val remainingMs = ingredient.expirationDate - currentTime
            val urgencyPerUnit = if (ingredient.expirationDate > 0) {
                val daysRemaining = remainingMs.toDouble() / (1000 * 60 * 60 * 24)
                if (daysRemaining <= 0) 10.0 else (1.0 / (daysRemaining + 1.0)) + 0.02
            } else {
                0.02
            }
            val weight = ingredient.quantity
            KnapsackItem(urgencyPerUnit * weight, weight, urgencyPerUnit)
        }
        val sortedItems = itemsWithDensity.sortedByDescending { it.ratio }
        var totalValue = 0.0
        var currentWeight = 0.0
        for (item in sortedItems) {
            if (currentWeight + item.weight <= capacity) {
                currentWeight += item.weight
                totalValue += item.value
            } else {
                val remaining = capacity - currentWeight
                if (item.weight > 0) totalValue += item.value * (remaining / item.weight)
                break
            }
        }
        return totalValue
    }

    private data class KnapsackItem(val value: Double, val weight: Double, val ratio: Double)
}

object RecipeRecommendationEngine {
    private fun String.normalize() = this.replace("\\s".toRegex(), "").lowercase()

    fun recommendTopRecipes(inventory: List<Ingredient>, recipes: List<Recipe>): List<RecommendedRecipe> {
        val currentTime = System.currentTimeMillis()
        val expirationBuffer = 60000L
        val fullInventoryMap = inventory.groupBy { it.name.normalize() }
        val usableInventoryMap = inventory
            .filter { it.expirationDate == 0L || (it.expirationDate + expirationBuffer) >= currentTime }
            .groupBy { it.name.normalize() }

        return recipes.map { recipe ->
            val recipeIngNamesNorm = recipe.ingredients.map { it.name.normalize() }
            val missing = getMissingIngredients(usableInventoryMap, recipe)
            val matchingInvItems = recipeIngNamesNorm.flatMap { name -> fullInventoryMap[name] ?: emptyList() }
            val spoiledInRecipe = matchingInvItems.filter {
                it.expirationDate > 0 && (it.expirationDate + expirationBuffer) < currentTime && it.quantity > 0
            }
            val hasSpoiled = spoiledInRecipe.isNotEmpty()

            val cappedInvForScore = mutableListOf<Ingredient>()
            recipe.ingredients.forEach { required ->
                val reqNameNorm = required.name.normalize()
                val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
                val batches = (usableInventoryMap[reqNameNorm] ?: emptyList())
                    .sortedWith(compareBy<Ingredient> { if (it.expirationDate == 0L) Long.MAX_VALUE else it.expirationDate })

                var takenBase = 0.0
                for (batch in batches) {
                    val batchBase = UnitConverter.toBaseUnit(batch.quantity, batch.unit)
                    val remainingNeeded = (requiredBase - takenBase).coerceAtLeast(0.0)
                    if (remainingNeeded <= 0) break
                    val toTake = minOf(batchBase, remainingNeeded)
                    if (toTake > 0) {
                        cappedInvForScore.add(batch.copy(quantity = UnitConverter.fromBaseUnit(toTake, batch.unit)))
                        takenBase += toTake
                    }
                }
            }

            val normalizedMatchingInv = cappedInvForScore.map { it.copy(quantity = UnitConverter.toBaseUnit(it.quantity, it.unit), unit = "g/ml") }
            val capacity = recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
            val score = if (hasSpoiled) 0.0 else RecommendationEngine.fractionalKnapsack(normalizedMatchingInv, capacity)
            val reasons = mutableListOf<String>()
            if (hasSpoiled) reasons.add("💀 Contains spoiled: ${spoiledInRecipe.joinToString { it.name }}")

            cappedInvForScore.filter { it.expirationDate > 0 && it.quantity > 0 }
                .sortedBy { it.expirationDate }.take(2).forEach { item ->
                    val daysLeft = ((item.expirationDate - currentTime).toDouble() / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        daysLeft == 0 -> reasons.add("🕒 ${item.name} expires today!")
                        daysLeft <= 3 -> reasons.add("🕒 ${item.name} expires in $daysLeft ${if (daysLeft == 1) "day" else "days"}")
                        else -> reasons.add("📦 Uses your ${item.name}")
                    }
                }

            if (reasons.isEmpty()) {
                if (missing.isEmpty()) reasons.add("✅ Ready to cook: All items in stock")
                else missing.forEach { reasons.add("⚠️ Need ${UnitConverter.formatDisplay(it.quantity, it.unit)} more ${it.name}") }
            }
            RecommendedRecipe(recipe, score, missing.isNotEmpty(), reasons, hasSpoiled, missing)
        }.sortedByDescending { it.urgencyScore }
    }

    private fun getMissingIngredients(usableInventoryMap: Map<String, List<Ingredient>>, recipe: Recipe): List<Ingredient> {
        val missing = mutableListOf<Ingredient>()
        for (required in recipe.ingredients) {
            val totalAvailableBase = (usableInventoryMap[required.name.normalize()] ?: emptyList()).sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
            val requiredBase = UnitConverter.toBaseUnit(required.quantity, required.unit)
            if (totalAvailableBase < (requiredBase - 0.01)) {
                missing.add(Ingredient(required.name, UnitConverter.fromBaseUnit(requiredBase - totalAvailableBase, required.unit), required.unit, 0L))
            }
        }
        return missing
    }
}