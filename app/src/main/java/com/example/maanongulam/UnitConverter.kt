package com.example.maanongulam

import java.util.Locale

object UnitConverter {
    /**
     * Normalizes a quantity to its base unit (g or ml).
     * Assumes 1kg = 1000g and 1L = 1000ml.
     */
    fun toBaseUnit(quantity: Double, unit: String): Double {
        return when (unit.lowercase()) {
            "kg", "l" -> quantity * 1000.0
            else -> quantity
        }
    }

    /**
     * Converts a base unit quantity (g or ml) back to a target unit.
     */
    fun fromBaseUnit(baseQuantity: Double, targetUnit: String): Double {
        return when (targetUnit.lowercase()) {
            "kg", "l" -> baseQuantity / 1000.0
            else -> baseQuantity
        }
    }

    /**
     * Deducts a needed quantity (with its own unit) from an inventory quantity (with its unit).
     * Returns the new quantity in the inventory's original unit.
     */
    fun calculateRemaining(
        currentQty: Double, 
        currentUnit: String, 
        neededQty: Double, 
        neededUnit: String
    ): Double {
        val currentBase = toBaseUnit(currentQty, currentUnit)
        val neededBase = toBaseUnit(neededQty, neededUnit)
        val remainingBase = (currentBase - neededBase).coerceAtLeast(0.0)
        return fromBaseUnit(remainingBase, currentUnit)
    }

    /**
     * Checks if current inventory is enough for needed quantity, regardless of units.
     */
    fun isEnough(
        currentQty: Double, 
        currentUnit: String, 
        neededQty: Double, 
        neededUnit: String
    ): Boolean {
        return toBaseUnit(currentQty, currentUnit) >= toBaseUnit(neededQty, neededUnit)
    }

    /**
     * Checks if the quantity is considered "low stock".
     * Threshold is 100 units in base (100g or 100ml).
     */
    fun isLowStock(quantity: Double, unit: String): Boolean {
        return toBaseUnit(quantity, unit) <= 100.0 && toBaseUnit(quantity, unit) > 0
    }
    
    /**
     * Calculates the shortfall in the needed unit's scale.
     */
    fun getShortfall(
        currentQty: Double, 
        currentUnit: String, 
        neededQty: Double, 
        neededUnit: String
    ): Double {
        val currentBase = toBaseUnit(currentQty, currentUnit)
        val neededBase = toBaseUnit(neededQty, neededUnit)
        val diffBase = (neededBase - currentBase).coerceAtLeast(0.0)
        return fromBaseUnit(diffBase, neededUnit)
    }

    /**
     * Automatically scales units for display (e.g., 1200g -> 1.2kg).
     */
    fun formatDisplay(quantity: Double, unit: String): String {
        val base = toBaseUnit(quantity, unit)
        return when {
            unit.lowercase() in listOf("g", "kg") && base >= 1000.0 -> {
                val value = base / 1000.0
                if (value == value.toInt().toDouble()) "${value.toInt()}kg" else "${String.format(Locale.getDefault(), "%.2f", value)}kg"
            }
            unit.lowercase() in listOf("ml", "l") && base >= 1000.0 -> {
                val value = base / 1000.0
                if (value == value.toInt().toDouble()) "${value.toInt()}L" else "${String.format(Locale.getDefault(), "%.2f", value)}L"
            }
            else -> {
                if (quantity == quantity.toInt().toDouble()) "${quantity.toInt()}$unit" else "${String.format(Locale.getDefault(), "%.2f", quantity)}$unit"
            }
        }
    }
}
