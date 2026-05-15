package com.example.maanongulam

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Recommendation : Screen("recommendation", "Ulam", { Icon(Icons.Default.Restaurant, null) })
    object AddIngredient : Screen("add_ingredient", "Inventory", { Icon(Icons.Default.Add, null) })
    object AddRecipe : Screen("add_recipe", "New Recipe", { Icon(Icons.AutoMirrored.Filled.List, null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val items = listOf(Screen.Recommendation, Screen.AddIngredient, Screen.AddRecipe)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recommendation.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Recommendation.route) { RecommendationScreen() }
            composable(Screen.AddIngredient.route) { IngredientInputScreen() }
            composable(Screen.AddRecipe.route) { RecipeInputScreen() }
        }
    }
}