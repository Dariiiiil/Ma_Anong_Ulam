package screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Recommendation : Screen("recommendation", "Ulam", { Icon(Icons.Default.Restaurant, null) })
    object AddIngredient : Screen("add_ingredient", "Inventory", { Icon(Icons.Default.Add, null) })
    object AddRecipe : Screen("add_recipe", "Recipe", { Icon(Icons.AutoMirrored.Filled.List, null) })
    object IngredientList : Screen("ingredient_list", "Full Inventory", { Icon(Icons.AutoMirrored.Filled.List, null) })
    object RecipeList : Screen("recipe_list", "All Recipes", { Icon(Icons.AutoMirrored.Filled.List, null) })
    object Restock : Screen("restock", "Restock", { Icon(Icons.Default.NotificationsActive, null) })
    object ShoppingList : Screen("shopping_list", "Shopping", { Icon(Icons.Default.ShoppingCart, null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val items = listOf(Screen.Recommendation, Screen.AddIngredient, Screen.AddRecipe, Screen.Restock, Screen.ShoppingList)
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
            composable(Screen.AddIngredient.route) { 
                IngredientInputScreen(onExpandList = { navController.navigate(Screen.IngredientList.route) }) 
            }
            composable(Screen.AddRecipe.route) { 
                RecipeInputScreen(
                    onExpandList = { navController.navigate(Screen.RecipeList.route) }
                ) 
            }
            composable(Screen.IngredientList.route) {
                IngredientListScreen(onBack = { navController.popBackStack() }) 
            }
            composable(Screen.RecipeList.route) { 
                RecipeListScreen(onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Restock.route) { 
                RestockScreen()
            }
            composable(Screen.ShoppingList.route) { 
                ShoppingListScreen()
            }
        }
    }
}
