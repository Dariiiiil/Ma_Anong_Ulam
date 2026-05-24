package com.example.maanongulam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PremadeRecipesScreen(
    viewModel: RecommendationViewModel = viewModel(),
    onRecipeAdded: () -> Unit = {}
) {
    val premadeRecipes = listOf(
        Recipe("Sinigang na Baboy", listOf(
            Ingredient("Pork", 500.0, "g", System.currentTimeMillis()),
            Ingredient("Tamarind Mix", 1.0, "g", 0L),
            Ingredient("Radish", 100.0, "g", System.currentTimeMillis()),
            Ingredient("Eggplant", 100.0, "g", System.currentTimeMillis()),
            Ingredient("String Beans", 50.0, "g", System.currentTimeMillis()),
            Ingredient("Water", 1.5, "L", 0L)
        )),
        Recipe("Chicken Curry", listOf(
            Ingredient("Chicken", 500.0, "g", System.currentTimeMillis()),
            Ingredient("Curry Powder", 20.0, "g", 0L),
            Ingredient("Coconut Milk", 200.0, "ml", 0L),
            Ingredient("Potato", 150.0, "g", System.currentTimeMillis()),
            Ingredient("Carrot", 100.0, "g", System.currentTimeMillis())
        )),
        Recipe("Pork Menudo", listOf(
            Ingredient("Pork", 500.0, "g", System.currentTimeMillis()),
            Ingredient("Tomato Sauce", 200.0, "ml", 0L),
            Ingredient("Liver", 100.0, "g", System.currentTimeMillis()),
            Ingredient("Potato", 100.0, "g", System.currentTimeMillis()),
            Ingredient("Carrot", 100.0, "g", System.currentTimeMillis())
        )),
        Recipe("Beef Pares", listOf(
            Ingredient("Beef", 500.0, "g", System.currentTimeMillis()),
            Ingredient("Star Anise", 2.0, "g", 0L),
            Ingredient("Soy Sauce", 100.0, "ml", 0L),
            Ingredient("Sugar", 50.0, "g", 0L),
            Ingredient("Garlic", 20.0, "g", 0L)
        ))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Premade Recipes", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Tap + to add to your collection", style = MaterialTheme.typography.bodyMedium)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(premadeRecipes) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = recipe.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${recipe.ingredients.size} Ingredients",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { 
                            viewModel.addRecipe(recipe.name, recipe.ingredients)
                            recipe.ingredients.forEach { ing ->
                                viewModel.addIngredientIfMissing(ing.name, ing.unit, ing.expirationDate)
                            }
                            onRecipeAdded()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Premade")
                        }
                    }
                }
            }
        }
    }
}
