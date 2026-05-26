package com.example.maanongulam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.util.*

import androidx.compose.material.icons.automirrored.filled.Sort

import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check

enum class SortType { NAME, CATEGORY, EXPIRY, STOCK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    viewModel: IngredientViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val ingredients by viewModel.ingredients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val showDeleteAllDialog = remember { mutableStateOf(value = false) }
    
    var sortType by remember { mutableStateOf(SortType.NAME) }
    var isAscending by remember { mutableStateOf(value = true) }
    var showSortMenu by remember { mutableStateOf(value = false) }

    if (showDeleteAllDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog.value = false },
            title = { Text("Delete All Ingredients") },
            text = { Text("Are you sure you want to clear your entire inventory? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllIngredients()
                        showDeleteAllDialog.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Full Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ingredients.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Options"
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Name") },
                                    onClick = { sortType = SortType.NAME; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortType == SortType.NAME) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Category") },
                                    onClick = { sortType = SortType.CATEGORY; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortType == SortType.CATEGORY) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Expiry") },
                                    onClick = { sortType = SortType.EXPIRY; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortType == SortType.EXPIRY) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Stock Level") },
                                    onClick = { sortType = SortType.STOCK; showSortMenu = false },
                                    trailingIcon = {
                                        if (sortType == SortType.STOCK) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (isAscending) "Ascending" else "Descending") },
                                    onClick = { isAscending = !isAscending; showSortMenu = false },
                                    trailingIcon = {
                                        Icon(
                                            if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteAllDialog.value = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Inventory") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val filteredIngredients = remember(ingredients, searchQuery, sortType, isAscending) {
                val filtered = ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
                val sorted = when (sortType) {
                    SortType.NAME -> filtered.sortedBy { it.name.lowercase() }
                    SortType.CATEGORY -> filtered.sortedBy { it.category.lowercase() }
                    SortType.EXPIRY -> filtered.sortedBy { 
                        if (it.expirationDate == 0L) Long.MAX_VALUE else it.expirationDate 
                    }
                    SortType.STOCK -> filtered.sortedBy { 
                        UnitConverter.toBaseUnit(it.quantity, it.unit)
                    }
                }
                if (isAscending) sorted else sorted.reversed()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sortLabel = when (sortType) {
                    SortType.NAME -> "Name"
                    SortType.CATEGORY -> "Category"
                    SortType.EXPIRY -> "Expiry"
                    SortType.STOCK -> "Stock Level"
                }
                val orderLabel = if (isAscending) "↑" else "↓"
                
                Text(
                    text = "Sorted by $sortLabel $orderLabel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${filteredIngredients.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredIngredients) { ingredient ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = ingredient.name, fontWeight = FontWeight.Bold)
                                    if (UnitConverter.isLowStock(ingredient.quantity, ingredient.unit)) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "LOW STOCK",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${UnitConverter.formatDisplay(ingredient.quantity, ingredient.unit)} • ${ingredient.category}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val expiry = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(Date(ingredient.expirationDate))
                                val expiryText = if (ingredient.expirationDate > 0) "Expires: $expiry" else "Non-perishable"
                                Text(
                                    text = expiryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteIngredient(ingredient) }) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: RecommendationViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val recipes by viewModel.allRecipes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val showDeleteAllDialog = remember { mutableStateOf(value = false) }

    if (showDeleteAllDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog.value = false },
            title = { Text("Delete All Recipes") },
            text = { Text("Are you sure you want to delete all your recipes? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllRecipes()
                        showDeleteAllDialog.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Recipes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recipes.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog.value = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Recipes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val filteredRecipes = remember(recipes, searchQuery) {
                recipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredRecipes) { recipe ->
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
}
