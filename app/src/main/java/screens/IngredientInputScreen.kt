package screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maanongulam.FoodDefinitionEntity
import com.example.maanongulam.IngredientEntity
import com.example.maanongulam.IngredientViewModel
import com.example.maanongulam.UnitConverter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientInputScreen(
    viewModel: IngredientViewModel = viewModel(),
    onExpandList: () -> Unit = {}
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val foodDefinitions by viewModel.foodDefinitions.collectAsState()
    
    var showAddIngredientWindow by remember { mutableStateOf(false) }
    var showAddNewFoodInternal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    val swipeEnabled = LocalPagerSwipeEnabled.current

    LaunchedEffect(showAddIngredientWindow) {
        swipeEnabled.value = !showAddIngredientWindow
    }

    // State for Adding/Editing Ingredient
    var selectedFood by remember { mutableStateOf<FoodDefinitionEntity?>(null) }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var isNonPerishable by remember { mutableStateOf(false) }
    var shelfLifeDays by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // State for Adding New Food Type
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("Others") }
    var newFoodUnitType by remember { mutableStateOf("MASS") } // MASS, VOLUME

    val dateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))

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

    // Reset ingredient form
    fun resetIngredientForm() {
        selectedFood = null
        quantity = ""
        unit = "g"
        isNonPerishable = false
        shelfLifeDays = ""
        editingIngredient = null
        showAddIngredientWindow = false
        showAddNewFoodInternal = false
    }

    // Handle Edit
    LaunchedEffect(editingIngredient) {
        editingIngredient?.let { ing ->
            val definition = foodDefinitions.find { it.name.equals(ing.name, true) }
            selectedFood = definition
            quantity = ing.quantity.toString()
            unit = ing.unit
            if (ing.expirationDate > 0) {
                isNonPerishable = false
                datePickerState.selectedDateMillis = ing.expirationDate
                val diff = ing.expirationDate - System.currentTimeMillis()
                shelfLifeDays = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0).toString()
            } else {
                isNonPerishable = true
                shelfLifeDays = ""
            }
            showAddIngredientWindow = true
        }
    }

    // Update isNonPerishable if food definition changes
    LaunchedEffect(selectedFood) {
        selectedFood?.let {
            if (it.isImperishable) {
                isNonPerishable = true
                shelfLifeDays = ""
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddIngredientWindow = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Ingredient")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Inventory") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Expiring Soon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onExpandList) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Expand List")
                }
            }

            val filteredIngredients = remember(ingredients, searchQuery) {
                val oneWeekMs = 7 * 24 * 60 * 60 * 1000L
                val currentTime = System.currentTimeMillis()
                
                val matchingPerishable = ingredients.filter { 
                    it.quantity > 0 && 
                    it.name.contains(searchQuery, ignoreCase = true) &&
                    it.expirationDate > 0
                }.sortedBy { it.expirationDate }

                val soon = matchingPerishable.filter { (it.expirationDate - currentTime) <= oneWeekMs }
                
                if (soon.size >= 5) soon else matchingPerishable.take(5)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredIngredients) { ingredient ->
                    val isLocked = foodDefinitions.find { it.name.equals(ingredient.name, true) }?.isImperishable == true
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (!isLocked) editingIngredient = ingredient },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = ingredient.name, fontWeight = FontWeight.Bold)
                                    if (isLocked) {
                                        Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.secondary)
                                    }
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
                                    val expiry = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ingredient.expirationDate))
                                    "Expires: $expiry"
                                } else {
                                    "Imperishable"
                                }
                                Text(
                                    text = expiryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteIngredient(ingredient) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // --- Add Ingredient Window ---
        if (showAddIngredientWindow) {
            Dialog(onDismissRequest = { resetIngredientForm() }) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingIngredient == null) "Add to Inventory" else "Edit Inventory Item",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showAddNewFoodInternal) {
                            // --- Internal Add New Food Form ---
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Define New Food Type", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                OutlinedTextField(
                                    value = newFoodName,
                                    onValueChange = { newFoodName = it },
                                    label = { Text("Food Name") },
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
                                            viewModel.addFoodDefinition(newFoodName, newFoodUnitType, newFoodCategory, false)
                                            newFoodName = ""
                                            showAddNewFoodInternal = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = newFoodName.isNotBlank()
                                    ) { Text("Create") }
                                }
                            }
                        } else {
                            // --- Standard Add Ingredient Form ---

                            // Searchable Dropdown for Food (Relevant 4 + Add Custom)
                            var expanded by remember { mutableStateOf(false) }
                            var foodSearch by remember { mutableStateOf(selectedFood?.name ?: "") }
                            
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
                                    label = { Text("Search Ingredient") },
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
                                                unit = when (definition.unitType) {
                                                    "VOLUME" -> "ml"
                                                    else -> "g"
                                                }
                                                expanded = false
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { 
                                                    viewModel.deleteFoodDefinition(definition)
                                                }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Delete Option", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        )
                                    }
                                    if (filteredDefinitions.isNotEmpty()) HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Add custom ingredient", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showAddNewFoodInternal = true
                                            expanded = false
                                        }
                                    )
                                }
                            }

                            // Quantity and Unit
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) {
                                            quantity = it
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
                                        Text(unit)
                                    }
                                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                        unitOptions.forEach { opt ->
                                            DropdownMenuItem(text = { Text(opt) }, onClick = { unit = opt; unitExpanded = false })
                                        }
                                    }
                                }
                            }

                            // Shelf Life (Only shown if NOT imperishable)
                            if (!isNonPerishable) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Shelf Life:", style = MaterialTheme.typography.labelLarge)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        FilterChip(
                                            selected = shelfLifeDays == "7",
                                            onClick = { shelfLifeDays = "7" },
                                            label = { Text("1 Week", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = shelfLifeDays == "14",
                                            onClick = { shelfLifeDays = "14" },
                                            label = { Text("2 Weeks", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = shelfLifeDays == "30",
                                            onClick = { shelfLifeDays = "30" },
                                            label = { Text("1 Month", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(dateText)
                                    }
                                }
                            }

                            if (showDatePicker) {
                                DatePickerDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
                                ) { DatePicker(state = datePickerState) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(onClick = { resetIngredientForm() }, modifier = Modifier.weight(1f)) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val food = selectedFood ?: return@Button
                                        val qty = quantity.toDoubleOrNull() ?: 0.0
                                        val expiry = if (isNonPerishable) 0L else (datePickerState.selectedDateMillis ?: System.currentTimeMillis())
                                        
                                        if (editingIngredient != null) {
                                            viewModel.updateIngredient(editingIngredient!!.copy(
                                                name = food.name, quantity = qty, unit = unit, expirationDate = expiry, category = food.category
                                            ))
                                        } else {
                                            viewModel.addIngredient(food.name, qty, unit, expiry, food.category)
                                        }
                                        resetIngredientForm()
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    enabled = selectedFood != null && quantity.isNotBlank()
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
