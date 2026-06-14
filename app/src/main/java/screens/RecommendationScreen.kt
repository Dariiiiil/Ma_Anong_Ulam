package screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.RecommendationViewModel
import com.example.maanongulam.RecommendedRecipe
import com.example.maanongulam.UnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val lastDeductions by viewModel.lastCookingDeductions.collectAsState()
    val lastFailedRecipe by viewModel.lastFailedRecipe.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val isNotificationsVisible = remember { mutableStateOf(true) }
    val isSafetyAlertExpanded = remember { mutableStateOf(false) }
    val isLowStockAlertExpanded = remember { mutableStateOf(false) }
    val showDeleteHistoryDialog = remember { mutableStateOf(false) }
    val showHistorySheet = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val swipeEnabled = LocalPagerSwipeEnabled.current
    
    var showSymbols by remember { mutableStateOf(true) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(cookingLog, showHistorySheet.value, showDeleteHistoryDialog.value, pendingDuplicates) {
        swipeEnabled.value = cookingLog == null && !showHistorySheet.value && !showDeleteHistoryDialog.value && pendingDuplicates == null
    }

    LaunchedEffect(recommendations) {
        // Auto-restock check disabled per user request
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
            title = { Text(if (lastFailedRecipe != null) "Incomplete Ingredients" else "Cooking Report") },
            text = { Column { cookingLog?.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = {
                if (lastFailedRecipe != null) {
                    Button(onClick = { viewModel.cookRecipe(lastFailedRecipe!!, force = true) }) {
                        Text("Cook Anyway")
                    }
                } else {
                    TextButton(onClick = { viewModel.clearCookingLog() }) { Text("OK") }
                }
            },
            dismissButton = {
                if (lastFailedRecipe != null) {
                    TextButton(onClick = { viewModel.clearCookingLog() }) { Text("Cancel") }
                }
            }
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
                title = { Text( text = "Recommendations",
                                fontWeight = FontWeight.Bold
                ) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        onClick = { viewModel.undoCook() },
                        enabled = lastDeductions != null,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Undo Cook")
                        }
                    }
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View History") },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                onClick = { 
                                    showOptionsMenu = false
                                    showHistorySheet.value = true 
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (showSymbols) "Show Scores as Numbers" else "Show Scores as Symbols") },
                                leadingIcon = { Icon(if (showSymbols) Icons.Default.Numbers else Icons.Default.Schedule, null) },
                                onClick = { 
                                    showSymbols = !showSymbols
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    // Just a visual refresh delay, no auto-adding items
                    delay(500)
                    isRefreshing = false 
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp, top = 10.dp)) {
                    itemsIndexed(recommendations) { index, item ->
                        if (index == 0) {
                            Text("Ulam of the Day", style = MaterialTheme.typography.titleLarge, color = if (item.hasSpoiledIngredients) Color.Gray else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                            RecipeCard(item, true, showSymbols) { viewModel.cookRecipe(item.recipe) }
                            if (recommendations.size > 1) Text("Runner-ups", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                        } else RecipeCard(item, false, showSymbols) { viewModel.cookRecipe(item.recipe) }
                    }
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
fun RecipeCard(item: RecommendedRecipe, isTop: Boolean, useSymbols: Boolean, onCook: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = when { item.hasSpoiledIngredients -> MaterialTheme.colorScheme.surfaceVariant; isTop -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surface })) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(item.recipe.name, style = if (isTop) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (item.hasSpoiledIngredients) Color.Gray else Color.Unspecified)
                if (item.hasSpoiledIngredients) Badge { Text("UNSAFE") }
                else if (item.isInsufficient) Badge(containerColor = MaterialTheme.colorScheme.errorContainer) { Text("Missing Items", color = MaterialTheme.colorScheme.error) }
            }
            if (!item.hasSpoiledIngredients) {
                if (useSymbols) {
                    val capacity = item.recipe.ingredients.sumOf { UnitConverter.toBaseUnit(it.quantity, it.unit) }
                    val normalizedScore = if (capacity > 0) (item.urgencyScore / capacity) * 10.0 else 0.0
                    UrgencyClockScale(normalizedScore)
                } else {
                    Text(
                        text = "Urgency Score: ${String.format(Locale.getDefault(), "%.4f", item.urgencyScore)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            item.reasons.forEach { Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), Arrangement.End) {
                Button(onCook, enabled = !item.hasSpoiledIngredients) { Text("Cook This") }
            }
        }
    }
}

@Composable
fun UrgencyClockScale(score: Double) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High contrast colors for better visibility
        val activeColor = MaterialTheme.colorScheme.primary
        val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

        repeat(10) { index ->
            val clockValue = index + 1
            val isFull = score >= clockValue
            val isHalf = !isFull && score >= (clockValue - 0.5)

            Box(contentAlignment = Alignment.Center) {
                // Background Gray Clock
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = inactiveColor
                )
                
                if (isFull) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = activeColor
                    )
                } else if (isHalf) {
                    // Half-filled clock using a clip
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(HalfSizeShape())
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = activeColor
                        )
                    }
                }
            }
        }
    }
}

private class HalfSizeShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Rectangle(Rect(0f, 0f, size.width / 2f, size.height))
    }
}
