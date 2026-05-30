package screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.IngredientViewModel
import com.example.maanongulam.ShoppingItem
import com.example.maanongulam.ShoppingViewModel
import com.example.maanongulam.UnitConverter

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
                actions = {
                    if (items.any { it.isChecked }) {
                        IconButton(onClick = { shoppingViewModel.clearCheckedItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Checked")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Your shopping list is empty.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                IconButton(onClick = { 
                                    val definition = foodDefinitions.find { it.name.equals(item.name, true) }
                                    ingredientViewModel.moveToInventory(item, definition)
                                }) {
                                    Icon(
                                        Icons.Default.Inventory, 
                                        contentDescription = "Move to Inventory", 
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { shoppingViewModel.deleteItem(item) }) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Remove", 
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
