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
            else -> quantity // g, ml remain as is
        }
    }

    /**
     * Converts a base unit quantity back to a target unit for display or calculation.
     */
    fun fromBaseUnit(baseQuantity: Double, targetUnit: String): Double {
        return when (targetUnit.lowercase().trim()) {
            "kg", "kilogram", "kilograms", "kilo" -> baseQuantity / 1000.0
            "l", "liter", "liters", "litre" -> baseQuantity / 1000.0
            else -> baseQuantity
        }
    }

    /**
     * Checks if the quantity is considered "low stock".
     * Threshold is set to 30% of standard estimated "full" capacities per category.
     */
    fun isLowStock(quantity: Double, unit: String, category: String = "Others"): Boolean {
        val base = toBaseUnit(quantity, unit)
        if (base <= 0) return false

        // Weight/Volume thresholds (30% of previous logic)
        return when (category.lowercase().trim()) {
            "spices" -> base <= 6.0    // e.g., < 6g/ml
            "grains" -> base <= 150.0  // e.g., < 150g (less than 1 cup of rice)
            "meat", "seafood" -> base <= 90.0  // e.g., < 90g
            "vegetables" -> base <= 60.0 // e.g., < 60g
            "dairy" -> base <= 60.0
            else -> base <= 30.0 // Default (30g/ml)
        }
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
