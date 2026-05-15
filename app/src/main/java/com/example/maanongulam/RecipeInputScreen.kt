package com.example.maanongulam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeInputScreen(
    viewModel: RecommendationViewModel = viewModel()
) {
    var recipeName by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<Pair<String, String>>() } // Name to Quantity string

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Create New Recipe", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = recipeName,
            onValueChange = { recipeName = it },
            label = { Text("Recipe Name") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        Text(text = "Ingredients", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ingredients) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item.first,
                        onValueChange = { ingredients[index] = it to item.second },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.second,
                        onValueChange = { ingredients[index] = item.first to it },
                        label = { Text("Qty (g/ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.6f)
                    )
                    IconButton(onClick = { ingredients.removeAt(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }

            item {
                TextButton(
                    onClick = { ingredients.add("" to "") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Ingredient")
                }
            }
        }

        Button(
            onClick = {
                val ingredientList = ingredients.mapNotNull {
                    val qty = it.second.toDoubleOrNull()
                    if (it.first.isNotBlank() && qty != null) {
                        Ingredient(it.first, qty, "g", 0L) // Expiration 0 for recipe templates
                    } else null
                }
                if (recipeName.isNotBlank() && ingredientList.isNotEmpty()) {
                    viewModel.addRecipe(Recipe(recipeName, ingredientList))
                    recipeName = ""
                    ingredients.clear()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = recipeName.isNotBlank() && ingredients.any { it.first.isNotBlank() && it.second.isNotBlank() }
        ) {
            Text("Save Recipe")
        }
    }
}