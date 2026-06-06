package screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RestockItem(
    val definition: FoodDefinitionEntity,
    val currentQuantity: Double,
    val currentUnit: String,
    val isOutOfStock: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    ingredientViewModel: IngredientViewModel = viewModel(),
    shoppingViewModel: ShoppingViewModel = viewModel()
) {
    val ingredients by ingredientViewModel.ingredients.collectAsState()
    val foodDefinitions by ingredientViewModel.foodDefinitions.collectAsState()

    var itemToRestock by remember { mutableStateOf<RestockItem?>(null) }
    var restockQuantity by remember { mutableStateOf("") }
    var restockUnit by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val swipeEnabled = LocalPagerSwipeEnabled.current

    LaunchedEffect(itemToRestock) {
        swipeEnabled.value = itemToRestock == null
    }

    val restockList = remember(ingredients, foodDefinitions) {
        foodDefinitions.mapNotNull { definition ->
            val inventoryItem = ingredients.find { it.name.equals(definition.name, ignoreCase = true) }
            
            val needsRestock = if (inventoryItem == null) {
                true // Out of stock
            } else {
                UnitConverter.isLowStock(inventoryItem.quantity, inventoryItem.unit, inventoryItem.category)
            }

            if (needsRestock) {
                RestockItem(
                    definition = definition,
                    currentQuantity = inventoryItem?.quantity ?: 0.0,
                    currentUnit = inventoryItem?.unit ?: when(definition.unitType) {
                        "VOLUME" -> "ml"
                        else -> "g"
                    },
                    isOutOfStock = inventoryItem == null
                )
            } else {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restock Center", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Items that are low or out of stock", style = MaterialTheme.typography.bodyMedium)

                if (restockList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("All stocked up! 🎉", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(restockList) { item ->
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
                                        Text(text = item.definition.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        val statusText = if (item.isOutOfStock) "OUT OF STOCK" else "LOW STOCK (${UnitConverter.formatDisplay(item.currentQuantity, item.currentUnit)})"
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    IconButton(onClick = { 
                                        itemToRestock = item
                                        restockUnit = item.currentUnit
                                        restockQuantity = ""
                                    }) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add to Shopping List", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Restock Quantity Dialog ---
    if (itemToRestock != null) {
        AlertDialog(
            onDismissRequest = { itemToRestock = null },
            title = { Text("Restock ${itemToRestock?.definition?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter quantity to add to your shopping list:")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = restockQuantity,
                            onValueChange = { 
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                    restockQuantity = it
                                }
                            },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        
                        val unitOptions = if (itemToRestock?.definition?.unitType == "VOLUME") listOf("ml", "L") else listOf("g", "kg")
                        var unitExpanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.weight(0.6f)) {
                            OutlinedButton(
                                onClick = { unitExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(restockUnit)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                unitOptions.forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = { restockUnit = opt; unitExpanded = false })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = restockQuantity.toDoubleOrNull() ?: 0.0
                        if (qty > 0) {
                            shoppingViewModel.addItem(itemToRestock!!.definition.name, qty, restockUnit)
                        }
                        itemToRestock = null
                    },
                    enabled = restockQuantity.isNotBlank() && (restockQuantity.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRestock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
