package com.example.maanongulam

/**
 * Represents a food ingredient with its quantity and expiration information.
 *
 * @property name The name of the ingredient.
 * @property quantity The amount of the ingredient.
 * @property unit The unit of measurement (e.g., "g", "ml"). Defaults to "g".
 * @property expirationDate The expiration date represented as a timestamp (Long).
 */
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: String = "g",
    val expirationDate: Long
)