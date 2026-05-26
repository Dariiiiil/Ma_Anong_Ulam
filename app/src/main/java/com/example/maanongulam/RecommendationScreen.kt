package com.example.maanongulam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    viewModel: RecommendationViewModel = viewModel()
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val historyLogs by viewModel.allCookingLogs.collectAsState()

    val isSafetyAlertExpanded = remember { mutableStateOf(false) }
    val isLowStockAlertExpanded = remember { mutableStateOf(false) }
    val isNotificationsVisible = remember { mutableStateOf(true) }
    
    // Auto-run the restock check when recommendations are loaded
    LaunchedEffect(recommendations) {
        if (recommendations.isNotEmpty()) {
            viewModel.runAutoRestockCheck()
        }
    }
    
    val showDeleteHistoryDialog = remember { mutableStateOf(false) }
    val showHistorySheet = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showDeleteHistoryDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteHistoryDialog.value = false },
            title = { Text("Clear Cooking History") },
            text = { Text("Are you sure you want to delete your entire cooking history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllCookingLogs()
                        showDeleteHistoryDialog.value = false
                        showHistorySheet.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHistoryDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showHistorySheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet.value = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Recent History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (historyLogs.isNotEmpty()) {
                        IconButton(onClick = { showDeleteHistoryDialog.value = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (historyLogs.isEmpty()) {
                    Text("No history yet. Start cooking to see your logs!")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(historyLogs) { _, log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = log.recipeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        val date = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                        Text(text = date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Safety check for spoiled items
    val spoiledIngredients = remember(recommendations) {
        recommendations.flatMap { it.reasons }
            .filter { it.contains("💀 Contains spoiled") }
            .map { it.replace("💀 Contains spoiled: ", "") }
            .distinct()
    }

    val lowStockItems = remember(allIngredients) {
        allIngredients.filter { UnitConverter.isLowStock(it.quantity, it.unit) }
    }
    
    val cookingLog by viewModel.cookingLog.collectAsState()

    if (cookingLog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearCookingLog() },
            title = { Text("Cooking Report") },
            text = {
                Column {
                    cookingLog?.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCookingLog() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ulam Recommendations") },
                actions = {
                    IconButton(onClick = { viewModel.runAutoRestockCheck() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Auto Restock Check")
                    }
                    IconButton(onClick = { showHistorySheet.value = true }) {
                        BadgedBox(
                            badge = { 
                                if (historyLogs.isNotEmpty()) {
                                    Badge { Text(historyLogs.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Cooking History")
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
                .padding(horizontal = 16.dp)
        ) {
            // Grouped Notifications Section
            if (spoiledIngredients.isNotEmpty() || lowStockItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alerts & Notifications",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    TextButton(
                        onClick = { isNotificationsVisible.value = !isNotificationsVisible.value },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 32.dp)
                    ) {
                        Text(
                            text = if (isNotificationsVisible.value) "Hide All" else "Show All",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                AnimatedVisibility(visible = isNotificationsVisible.value) {
                    Column {
                        if (spoiledIngredients.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                onClick = { isSafetyAlertExpanded.value = !isSafetyAlertExpanded.value }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Safety Alert: Spoiled items!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            if (isSafetyAlertExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    AnimatedVisibility(visible = isSafetyAlertExpanded.value) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            spoiledIngredients.forEach { name ->
                                                Text(
                                                    text = "• $name",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (lowStockItems.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                onClick = { isLowStockAlertExpanded.value = !isLowStockAlertExpanded.value }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Low Stock: ${lowStockItems.size} items",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            if (isLowStockAlertExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    AnimatedVisibility(visible = isLowStockAlertExpanded.value) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            lowStockItems.forEach { item ->
                                                Text(
                                                    text = "• ${item.name} (${UnitConverter.formatDisplay(item.quantity, item.unit)})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (recommendations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recommendations. Add ingredients and recipes!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(recommendations) { index, item ->
                        if (index == 0) {
                            Text(
                                text = "Ulam of the Day",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (item.hasSpoiledIngredients) Color.Gray else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            RecipeCard(
                                recommendedRecipe = item,
                                isTopPick = true,
                                onCookClick = { viewModel.cookRecipe(item.recipe) },
                                onAddToShoppingList = { viewModel.addMissingToShoppingList(item.missingIngredients) }
                            )
                            if (recommendations.size > 1) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Runners-up",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } else {
                                RecipeCard(
                                recommendedRecipe = item,
                                isTopPick = false,
                                onCookClick = { viewModel.cookRecipe(item.recipe) },
                                onAddToShoppingList = { viewModel.addMissingToShoppingList(item.missingIngredients) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recommendedRecipe: RecommendedRecipe,
    isTopPick: Boolean,
    onCookClick: () -> Unit,
    onAddToShoppingList: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = when {
            recommendedRecipe.hasSpoiledIngredients -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            isTopPick -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else -> CardDefaults.cardColors()
        },
        elevation = if (isTopPick && !recommendedRecipe.hasSpoiledIngredients) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendedRecipe.recipe.name,
                    style = if (isTopPick) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (recommendedRecipe.hasSpoiledIngredients) Color.Gray else Color.Unspecified
                )
                
                if (recommendedRecipe.hasSpoiledIngredients) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "UNSAFE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                } else if (recommendedRecipe.isInsufficient) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Insufficient",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Missing Ingredients",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            if (!recommendedRecipe.hasSpoiledIngredients) {
                Text(
                    text = "Urgency Score: ${String.format(Locale.getDefault(), "%.4f", recommendedRecipe.urgencyScore)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (recommendedRecipe.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                recommendedRecipe.reasons.forEach { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (recommendedRecipe.hasSpoiledIngredients) MaterialTheme.colorScheme.error 
                                else if (isTopPick) MaterialTheme.colorScheme.onPrimaryContainer 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recommendedRecipe.isInsufficient && !recommendedRecipe.hasSpoiledIngredients) {
                    TextButton(onClick = onAddToShoppingList) {
                        Text("Add to Shopping List")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Button(
                    onClick = onCookClick,
                    enabled = !recommendedRecipe.hasSpoiledIngredients,
                    colors = if (recommendedRecipe.isInsufficient || recommendedRecipe.hasSpoiledIngredients)
                        ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    else
                        ButtonDefaults.buttonColors()
                ) {
                    Text("Cook This")
                }
            }
        }
    }
}
