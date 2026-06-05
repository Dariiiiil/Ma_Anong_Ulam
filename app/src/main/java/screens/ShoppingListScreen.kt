package screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.FoodDefinitionEntity
import com.example.maanongulam.IngredientViewModel
import com.example.maanongulam.ShoppingItem
import com.example.maanongulam.ShoppingViewModel
import com.example.maanongulam.UnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingViewModel: ShoppingViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel()
) {
    val items by shoppingViewModel.shoppingItems.collectAsState()
    val foodDefinitions by ingredientViewModel.foodDefinitions.collectAsState()

    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var editQuantity by remember { mutableStateOf("") }
    var editUnit by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val swipeEnabled = LocalPagerSwipeEnabled.current
    
    // Add Item State
    var showAddItemWindow by remember { mutableStateOf(false) }
    
    LaunchedEffect(showAddItemWindow, itemToEdit) {
        swipeEnabled.value = !showAddItemWindow && itemToEdit == null
    }

    var showAddNewFoodInternal by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodDefinitionEntity?>(null) }
    var addQuantity by remember { mutableStateOf("") }
    var addUnit by remember { mutableStateOf("g") }

    // State for Adding New Food Type
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("Others") }
    var newFoodUnitType by remember { mutableStateOf("MASS") } // MASS, VOLUME

    val hasCheckedItems = items.any { it.isChecked }

    fun resetAddForm() {
        selectedFood = null
        addQuantity = ""
        addUnit = "g"
        showAddItemWindow = false
        showAddNewFoodInternal = false
        newFoodName = ""
        newFoodCategory = "Others"
        newFoodUnitType = "MASS"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
                actions = {
                    IconButton(
                        onClick = { 
                            items.filter { it.isChecked }.forEach { item ->
                                val definition = foodDefinitions.find { it.name.equals(item.name, true) }
                                ingredientViewModel.moveToInventory(item, definition)
                            }
                        },
                        enabled = hasCheckedItems
                    ) {
                        Icon(
                            Icons.Default.Kitchen, 
                            contentDescription = "Move to Inventory",
                            tint = if (hasCheckedItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = { shoppingViewModel.clearCheckedItems() },
                        enabled = hasCheckedItems
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Clear Checked",
                            tint = if (hasCheckedItems) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showAddItemWindow = true },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Item")
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your shopping list is empty.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { shoppingViewModel.toggleChecked(item) }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                    )
                                    Text(
                                        text = UnitConverter.formatDisplay(item.quantity, item.unit),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.clickable {
                                            itemToEdit = item
                                            editQuantity = item.quantity.toString()
                                            editUnit = item.unit
                                        }
                                    )
                                }
                                
                                Row {
                                    IconButton(onClick = { 
                                        itemToEdit = item
                                        editQuantity = item.quantity.toString()
                                        editUnit = item.unit
                                    }) {
                                        Icon(Icons.Default.Edit, "Edit Quantity", tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Item Dialog ---
    if (showAddItemWindow) {
        Dialog(onDismissRequest = { resetAddForm() }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Item",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (showAddNewFoodInternal) {
                        // --- Internal Add New Food Form ---
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Define New Item Type", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = newFoodName,
                                onValueChange = { newFoodName = it },
                                label = { Text("Item Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            var catExpanded by remember { mutableStateOf(false) }
                            val categories = listOf("Meat", "Vegetables", "Seafood", "Dairy", "Spices", "Grains", "Others")
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Category: $newFoodCategory")
                                }
                                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    categories.forEach { c ->
                                        DropdownMenuItem(text = { Text(c) }, onClick = { newFoodCategory = c; catExpanded = false })
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = newFoodUnitType == "MASS", onClick = { newFoodUnitType = "MASS" }, label = { Text("Mass (g/kg)") })
                                FilterChip(selected = newFoodUnitType == "VOLUME", onClick = { newFoodUnitType = "VOLUME" }, label = { Text("Volume (ml/L)") })
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showAddNewFoodInternal = false }, modifier = Modifier.weight(1f)) { Text("Back") }
                                Button(
                                    onClick = {
                                        ingredientViewModel.addFoodDefinition(newFoodName, newFoodUnitType, newFoodCategory, false)
                                        newFoodName = ""
                                        showAddNewFoodInternal = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = newFoodName.isNotBlank()
                                ) { Text("Create") }
                            }
                        }
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        var foodSearch by remember { mutableStateOf("") }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = foodSearch,
                                onValueChange = { 
                                    foodSearch = it
                                    expanded = true
                                },
                                label = { Text("Search Item") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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
                                            selectedFood = definition
                                            foodSearch = definition.name
                                            addUnit = when (definition.unitType) {
                                                "VOLUME" -> "ml"
                                                else -> "g"
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                                if (filteredDefinitions.isNotEmpty()) HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Add new item type", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showAddNewFoodInternal = true
                                        expanded = false
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = addQuantity,
                                onValueChange = { 
                                    if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                        addQuantity = it
                                    }
                                },
                                label = { Text("Quantity") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            
                            val unitOptions = if (selectedFood?.unitType == "VOLUME") listOf("ml", "L") else listOf("g", "kg")
                            var unitExpanded by remember { mutableStateOf(false) }
                            
                            Box(modifier = Modifier.weight(0.6f)) {
                                OutlinedButton(
                                    onClick = { unitExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Text(addUnit)
                                }
                                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                    unitOptions.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { addUnit = opt; unitExpanded = false })
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = { resetAddForm() }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val foodName = foodSearch
                                    val qty = addQuantity.toDoubleOrNull() ?: 0.0
                                    if (foodName.isNotBlank() && qty > 0) {
                                        shoppingViewModel.addItem(foodName, qty, addUnit)
                                    }
                                    resetAddForm()
                                },
                                modifier = Modifier.weight(1.5f),
                                enabled = foodSearch.isNotBlank() && addQuantity.isNotBlank()
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Edit Quantity Dialog ---
    if (itemToEdit != null) {
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit ${itemToEdit?.name}") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = editQuantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                editQuantity = it
                            }
                        },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    
                    val definition = foodDefinitions.find { it.name.equals(itemToEdit?.name, true) }
                    val unitOptions = if (definition?.unitType == "VOLUME") listOf("ml", "L") else listOf("g", "kg")
                    var unitExpanded by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.weight(0.6f)) {
                        OutlinedButton(
                            onClick = { unitExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(editUnit)
                        }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            unitOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { editUnit = opt; unitExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = editQuantity.toDoubleOrNull() ?: 0.0
                        if (qty > 0) {
                            shoppingViewModel.updateItem(itemToEdit!!.copy(quantity = qty, unit = editUnit))
                        }
                        itemToEdit = null
                    },
                    enabled = editQuantity.isNotBlank() && (editQuantity.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
