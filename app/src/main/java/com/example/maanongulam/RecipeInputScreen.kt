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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeInputScreen(
    viewModel: RecommendationViewModel = viewModel(),
    onExpandList: () -> Unit = {}
) {
    val recipes by viewModel.allRecipes.collectAsState()
    
    var recipeName by remember { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<Pair<String, String>>() } // Name to Quantity string

    var editingRecipe by remember { mutableStateOf<RecipeEntity?>(null) }

    // Update form when editingRecipe changes
    LaunchedEffect(editingRecipe) {
        editingRecipe?.let {
            recipeName = it.name
            ingredients.clear()
            it.ingredients.forEach { ing ->
                ingredients.add(ing.name to ing.quantity.toString())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (editingRecipe == null) "Create New Recipe" else "Edit Recipe",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = recipeName,
            onValueChange = { recipeName = it },
            label = { Text("Recipe Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Ingredients", style = MaterialTheme.typography.titleMedium)

        // Recipe Input Area
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
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
                            label = { Text("Qty") },
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (editingRecipe != null) {
                OutlinedButton(
                    onClick = {
                        editingRecipe = null
                        recipeName = ""
                        ingredients.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = {
                    val ingredientList = ingredients.mapNotNull {
                        val qty = it.second.toDoubleOrNull()
                        if (it.first.isNotBlank() && qty != null) {
                            Ingredient(it.first, qty, "g", 0L)
                        } else null
                    }
                    
                    val currentEdit = editingRecipe
                    if (currentEdit != null) {
                        viewModel.updateRecipe(
                            currentEdit.copy(name = recipeName, ingredients = ingredientList)
                        )
                    } else {
                        viewModel.addRecipe(recipeName, ingredientList)
                    }
                    
                    recipeName = ""
                    ingredients.clear()
                    editingRecipe = null
                },
                modifier = Modifier.weight(1f),
                enabled = recipeName.isNotBlank() && ingredients.any { it.first.isNotBlank() && it.second.isNotBlank() }
            ) {
                Text(if (editingRecipe == null) "Save Recipe" else "Update Recipe")
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recipe List", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onExpandList) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Expand List")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipes) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { editingRecipe = recipe },
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
                        IconButton(onClick = { viewModel.deleteRecipe(recipe) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
