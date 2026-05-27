package com.example.maanongulam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(viewModel: RecommendationViewModel = viewModel()) {
    val recommendations by viewModel.recommendations.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val historyLogs by viewModel.allCookingLogs.collectAsState()
    val cookingLog by viewModel.cookingLog.collectAsState()
    val pendingDuplicates by viewModel.pendingShoppingDuplicates.collectAsState()

    val isNotificationsVisible = remember { mutableStateOf(true) }
    val isSafetyAlertExpanded = remember { mutableStateOf(false) }
    val isLowStockAlertExpanded = remember { mutableStateOf(false) }
    val showDeleteHistoryDialog = remember { mutableStateOf(false) }
    val showHistorySheet = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(recommendations) {
        if (recommendations.isNotEmpty()) viewModel.runAutoRestockCheck()
    }

    if (pendingDuplicates != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissShoppingDuplicates() },
            title = { Text("Duplicate Items Found") },
            text = {
                Column {
                    Text("The following items are already in your shopping list:")
                    Spacer(modifier = Modifier.height(8.dp))
                    pendingDuplicates?.forEach { (existing, newItem) ->
                        Text("• ${existing.name}: Have ${UnitConverter.formatDisplay(existing.quantity, existing.unit)}, adding ${UnitConverter.formatDisplay(newItem.quantity, newItem.unit)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Would you like to merge these quantities?")
                }
            },
            confirmButton = { Button(onClick = { viewModel.confirmShoppingDuplicates(merge = true) }) { Text("Merge All") } },
            dismissButton = { TextButton(onClick = { viewModel.confirmShoppingDuplicates(merge = false) }) { Text("Keep Existing Only") } }
        )
    }

    if (cookingLog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearCookingLog() },
            title = { Text("Cooking Report") },
            text = { Column { cookingLog?.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = { TextButton(onClick = { viewModel.clearCookingLog() }) { Text("OK") } }
        )
    }

    if (showDeleteHistoryDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteHistoryDialog.value = false },
            title = { Text("Clear Cooking History") },
            text = { Text("Are you sure you want to delete your entire cooking history?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteAllCookingLogs(); showDeleteHistoryDialog.value = false; showHistorySheet.value = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear All") } },
            dismissButton = { TextButton(onClick = { showDeleteHistoryDialog.value = false }) { Text("Cancel") } }
        )
    }

    if (showHistorySheet.value) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet.value = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Recent History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (historyLogs.isNotEmpty()) IconButton(onClick = { showDeleteHistoryDialog.value = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (historyLogs.isEmpty()) Text("No history yet.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(historyLogs) { _, log ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = log.recipeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ulam Recommendations") },
                actions = {
                    IconButton(onClick = { viewModel.runAutoRestockCheck() }) { Icon(Icons.Default.Refresh, null) }
                    IconButton(onClick = { showHistorySheet.value = true }) {
                        BadgedBox(badge = { if (historyLogs.isNotEmpty()) Badge { Text(historyLogs.size.toString()) } }) { Icon(Icons.Default.History, null) }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            val spoiled = recommendations.flatMap { it.reasons }.filter { it.contains("💀 Contains spoiled") }.map { it.replace("💀 Contains spoiled: ", "") }.distinct()
            val lowStock = allIngredients.filter { UnitConverter.isLowStock(it.quantity, it.unit, it.category) }

            if (spoiled.isNotEmpty() || lowStock.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Alerts & Notifications", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    TextButton(onClick = { isNotificationsVisible.value = !isNotificationsVisible.value }) { Text(if (isNotificationsVisible.value) "Hide All" else "Show All", style = MaterialTheme.typography.labelSmall) }
                }
                AnimatedVisibility(visible = isNotificationsVisible.value) {
                    Column {
                        if (spoiled.isNotEmpty()) AlertCard("Safety Alert: Spoiled items!", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, isSafetyAlertExpanded) {
                            spoiled.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (lowStock.isNotEmpty()) AlertCard("Low Stock: ${lowStock.size} items", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary, isLowStockAlertExpanded) {
                            lowStock.forEach { Text("• ${it.name} (${UnitConverter.formatDisplay(it.quantity, it.unit)})", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            if (recommendations.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No recommendations.") }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(recommendations) { index, item ->
                    if (index == 0) {
                        Text("Ulam of the Day", style = MaterialTheme.typography.titleLarge, color = if (item.hasSpoiledIngredients) Color.Gray else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                        RecipeCard(item, true, { viewModel.cookRecipe(item.recipe) }, { viewModel.addMissingToShoppingList(item.missingIngredients) })
                        if (recommendations.size > 1) Text("Runners-up", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                    } else RecipeCard(item, false, { viewModel.cookRecipe(item.recipe) }, { viewModel.addMissingToShoppingList(item.missingIngredients) })
                }
            }
        }
    }
}

@Composable
fun AlertCard(title: String, containerColor: Color, contentColor: Color, expanded: MutableState<Boolean>, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { expanded.value = !expanded.value }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = contentColor)
                Spacer(Modifier.width(12.dp))
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Icon(if (expanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = contentColor)
            }
            AnimatedVisibility(expanded.value) { Column(Modifier.padding(top = 8.dp)) { content() } }
        }
    }
}

@Composable
fun RecipeCard(item: RecommendedRecipe, isTop: Boolean, onCook: () -> Unit, onShop: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = when { item.hasSpoiledIngredients -> MaterialTheme.colorScheme.surfaceVariant; isTop -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surface })) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(item.recipe.name, style = if (isTop) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (item.hasSpoiledIngredients) Color.Gray else Color.Unspecified)
                if (item.hasSpoiledIngredients) Badge { Text("UNSAFE") }
                else if (item.isInsufficient) Badge(containerColor = MaterialTheme.colorScheme.errorContainer) { Text("Missing Items", color = MaterialTheme.colorScheme.error) }
            }
            if (!item.hasSpoiledIngredients) Text("Urgency Score: ${String.format(Locale.getDefault(), "%.4f", item.urgencyScore)}", style = MaterialTheme.typography.bodySmall)
            item.reasons.forEach { Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), Arrangement.End) {
                if (item.isInsufficient && !item.hasSpoiledIngredients) TextButton(onShop) { Text("Add to Shopping List") }
                Button(onCook, enabled = !item.hasSpoiledIngredients) { Text("Cook This") }
            }
        }
    }
}
