package com.example.maanongulam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).maAnongUlamDao()

    val shoppingItems: StateFlow<List<ShoppingItem>> = dao.getAllShoppingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch {
            dao.insertShoppingItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            dao.deleteShoppingItem(item)
        }
    }

    fun clearCheckedItems() {
        viewModelScope.launch {
            shoppingItems.value.filter { it.isChecked }.forEach {
                dao.deleteShoppingItem(it)
            }
        }
    }
}
