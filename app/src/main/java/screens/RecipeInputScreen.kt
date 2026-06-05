package screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeInputScreen(
    viewModel: RecommendationViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onExpandList: () -> Unit = {}
) {
    val recipes by viewModel.allRecipes.collectAsState()
    val foodDefinitions by ingredientViewModel.foodDefinitions.collectAsState()
    
    var showRecipeWindow by remember { mutableStateOf(false) }
    var recipeName by remember { mutableStateOf("") }
    // Triple: Name, Quantity, Unit
    val ingredients = remember { mutableStateListOf<Triple<String, String, String>>() } 

    var editingRecipe by remember { mutableStateOf<RecipeEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val swipeEnabled = LocalPagerSwipeEnabled.current

    LaunchedEffect(showRecipeWindow) {
        swipeEnabled.value = !showRecipeWindow
    }

    // Update form when editingRecipe changes
    LaunchedEffect(editingRecipe) {
        editingRecipe?.let {
            recipeName = it.name
            ingredients.clear()
            it.ingredients.forEach { ing ->
                ingredients.add(Triple(ing.name, ing.quantity.toString(), ing.unit))
            }
            showRecipeWindow = true
        }
    }

    fun resetForm() {
        recipeName = ""
        ingredients.clear()
        editingRecipe = null
        showRecipeWindow = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { showRecipeWindow = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Recipe")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Recipes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recipe List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onExpandList) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Expand List")
            }
        }

        val filteredRecipes = remember(recipes, searchQuery) {
            recipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRecipes) { recipe ->
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

    if (showRecipeWindow) {
        Dialog(onDismissRequest = { resetForm() }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (editingRecipe == null) "Create Recipe" else "Edit Recipe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = recipeName,
                        onValueChange = { recipeName = it },
                        label = { Text("Recipe Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Ingredients", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(ingredients) { index, item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            var expanded by remember { mutableStateOf(false) }
                                            var foodSearch by remember { mutableStateOf(item.first) }
                                            
                                            ExposedDropdownMenuBox(
                                                expanded = expanded,
                                                onExpandedChange = { expanded = it },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                OutlinedTextField(
                                                    value = foodSearch,
                                                    onValueChange = { 
                                                        foodSearch = it
                                                        expanded = true
                                                        ingredients[index] = Triple(it, item.second, item.third)
                                                    },
                                                    label = { Text("Ingredient Name") },
                                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                                    singleLine = true
                                                )
                                                
                                                val filteredDefinitions = foodDefinitions
                                                    .filter { it.name.contains(foodSearch, ignoreCase = true) }
                                                    .take(4)

                                                ExposedDropdownMenu(
                                                    expanded = expanded,
                                                    onDismissRequest = { expanded = false }
                                                ) {
                                                    filteredDefinitions.forEach { definition ->
                                                        DropdownMenuItem(
                                                            text = { Text(definition.name) },
                                                            onClick = {
                                                                val newUnit = if (definition.unitType == "VOLUME") "ml" else "g"
                                                                ingredients[index] = Triple(definition.name, item.second, newUnit)
                                                                foodSearch = definition.name
                                                                expanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            IconButton(onClick = { ingredients.removeAt(index) }) {
                                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = item.second,
                                                onValueChange = { 
                                                    if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                                        ingredients[index] = Triple(item.first, it, item.third)
                                                    }
                                                },
                                                label = { Text("Quantity") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true
                                            )
                                            
                                            val definition = foodDefinitions.find { it.name.equals(item.first, true) }
                                            val unitOptions = if (definition?.unitType == "VOLUME") listOf("ml", "L") else listOf("g", "kg")
                                            var unitExpanded by remember { mutableStateOf(false) }
                                            
                                            Box(modifier = Modifier.weight(0.6f)) {
                                                OutlinedButton(
                                                    onClick = { unitExpanded = true },
                                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                                ) {
                                                    Text(item.third)
                                                }
                                                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                                    unitOptions.forEach { opt ->
                                                        DropdownMenuItem(text = { Text(opt) }, onClick = { 
                                                            ingredients[index] = Triple(item.first, item.second, opt)
                                                            unitExpanded = false 
                                                        })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                TextButton(
                                    onClick = { ingredients.add(Triple("", "", "g")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Ingredient")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { resetForm() }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val ingredientList = ingredients.mapNotNull {
                                    val qty = it.second.toDoubleOrNull()
                                    if (it.first.isNotBlank() && qty != null) {
                                        Ingredient(it.first, qty, it.third, 0L)
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
                                resetForm()
                            },
                            modifier = Modifier.weight(1.5f),
                            enabled = recipeName.isNotBlank() && ingredients.any { it.first.isNotBlank() && it.second.isNotBlank() }
                        ) {
                            Text(if (editingRecipe == null) "Save Recipe" else "Update Recipe")
                        }
                    }
                }
            }
        }
    }
}
