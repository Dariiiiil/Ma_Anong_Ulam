package screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.example.maanongulam.IngredientEntity
import com.example.maanongulam.IngredientViewModel
import com.example.maanongulam.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientInputScreen(
    viewModel: IngredientViewModel = viewModel(),
    onExpandList: () -> Unit = {}
) {
    val ingredients by viewModel.ingredients.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var expanded by remember { mutableStateOf(false) }
    val units = listOf("g", "kg", "ml", "L")

    var category by remember { mutableStateOf("Others") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Vegetables", "Meat", "Seafood", "Dairy", "Spices", "Grains", "Others")

    var editingIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var isNonPerishable by remember { mutableStateOf(false) }
    var shelfLifeDays by remember { mutableStateOf("") }
    
    var isFormExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return utcTimeMillis >= calendar.timeInMillis
            }
        }
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))

    // Update form when editingIngredient changes
    LaunchedEffect(editingIngredient) {
        editingIngredient?.let {
            name = it.name
            quantity = it.quantity.toString()
            unit = it.unit
            category = it.category
            if (it.expirationDate > 0) {
                isNonPerishable = false
                datePickerState.selectedDateMillis = it.expirationDate
            } else {
                isNonPerishable = true
            }
            isFormExpanded = true // Auto-expand when editing
        }
    }

    // Sync date picker with shelf life days
    LaunchedEffect(shelfLifeDays) {
        val days = shelfLifeDays.toLongOrNull()
        if (days != null && days >= 0) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, days.toInt())
            }
            datePickerState.selectedDateMillis = calendar.timeInMillis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collapsible Form Header
        Surface(
            onClick = { isFormExpanded = !isFormExpanded },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingIngredient == null) "Add New Ingredient" else "Edit Ingredient",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(visible = isFormExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Category: $category")
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    category = selection
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = quantity.toDoubleOrNull() ?: 0.0
                                if (current > 0) quantity = (current - 1).coerceAtLeast(0.0).toString()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                        }
                        
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val current = quantity.toDoubleOrNull() ?: 0.0
                                quantity = (current + 1).toString()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                        }
                    }

                    Box(modifier = Modifier.weight(0.6f)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(unit)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            units.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        unit = selection
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isNonPerishable,
                        onCheckedChange = { isNonPerishable = it }
                    )
                    Text("Non-perishable (No Expiry)")
                }

                if (!isNonPerishable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = shelfLifeDays,
                            onValueChange = { shelfLifeDays = it },
                            label = { Text("Shelf Life (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text(dateText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("OK")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingIngredient != null) {
                        OutlinedButton(
                            onClick = {
                                editingIngredient = null
                                name = ""
                                quantity = ""
                                unit = "g"
                                category = "Others"
                                shelfLifeDays = ""
                                isFormExpanded = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }

                    Button(
                        onClick = {
                            val qty = quantity.toDoubleOrNull() ?: 0.0
                            val finalExpiration = if (isNonPerishable) 0L else selectedDate
                            val currentEdit = editingIngredient
                            if (currentEdit != null) {
                                viewModel.updateIngredient(
                                    currentEdit.copy(
                                        name = name,
                                        quantity = qty,
                                        unit = unit,
                                        expirationDate = finalExpiration,
                                        category = category
                                    )
                                )
                            } else {
                                viewModel.addIngredient(name, qty, unit, finalExpiration, category)
                            }
                            name = ""
                            quantity = ""
                            unit = "g"
                            category = "Others"
                            isNonPerishable = false
                            shelfLifeDays = ""
                            editingIngredient = null
                            isFormExpanded = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && quantity.isNotBlank()
                    ) {
                        Text(if (editingIngredient == null) "Add Ingredient" else "Update")
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Inventory") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Current Inventory", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onExpandList) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Expand List")
            }
        }

        val filteredIngredients = remember(ingredients, searchQuery) {
            ingredients.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredIngredients) { ingredient ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { editingIngredient = ingredient },
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
                                if (UnitConverter.isLowStock(ingredient.quantity, ingredient.unit, ingredient.category)) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "LOW",
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
                            val expiryText = if (ingredient.expirationDate > 0) {
                                val expiry = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    .format(Date(ingredient.expirationDate))
                                "Expires: $expiry"
                            } else {
                                "Non-perishable"
                            }
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
