package com.example.maanongulam

import java.util.Locale

object UnitConverter {
    /**
     * Normalizes a quantity to its base unit (g, ml, or pcs).
     * Handles common cooking unit variations and case sensitivity.
     */
    fun toBaseUnit(quantity: Double, unit: String): Double {
        return when (unit.lowercase().trim()) {
            "kg", "kilogram", "kilograms", "kilo" -> quantity * 1000.0
            "l", "liter", "liters", "litre" -> quantity * 1000.0
            "ml", "milliliter", "milliliters" -> quantity
            "g", "gram", "grams" -> quantity
            "tsp", "teaspoon", "teaspoons" -> quantity * 5.0
            "tbsp", "tablespoon", "tablespoons" -> quantity * 15.0
            "cup", "cups" -> quantity * 240.0
            "oz", "ounce", "ounces" -> quantity * 28.35
            "lb", "pound", "pounds" -> quantity * 453.59
            else -> quantity // pcs, packs, etc. remain as is
        }
    }

    /**
     * Converts a base unit quantity back to a target unit for display or calculation.
     */
    fun fromBaseUnit(baseQuantity: Double, targetUnit: String): Double {
        return when (targetUnit.lowercase().trim()) {
            "kg", "kilogram", "kilograms", "kilo" -> baseQuantity / 1000.0
            "l", "liter", "liters", "litre" -> baseQuantity / 1000.0
            "tsp", "teaspoon", "teaspoons" -> baseQuantity / 5.0
            "tbsp", "tablespoon", "tablespoons" -> baseQuantity / 15.0
            "cup", "cups" -> baseQuantity / 240.0
            "oz", "ounce", "ounces" -> baseQuantity / 28.35
            "lb", "pound", "pounds" -> baseQuantity / 453.59
            else -> baseQuantity
        }
    }

    /**
     * Checks if the quantity is considered "low stock".
     * Threshold is 100 units in base (100g or 100ml).
     */
    fun isLowStock(quantity: Double, unit: String): Boolean {
        return toBaseUnit(quantity, unit) <= 100.0 && toBaseUnit(quantity, unit) > 0
    }

    /**
     * Automatically scales units for display (e.g., 1200g -> 1.2kg).
     */
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
