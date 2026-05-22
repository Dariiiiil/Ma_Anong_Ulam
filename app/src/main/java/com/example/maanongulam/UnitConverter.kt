package com.example.maanongulam

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
}
