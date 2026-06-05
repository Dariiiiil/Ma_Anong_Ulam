package screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

val LocalPagerSwipeEnabled = compositionLocalOf { mutableStateOf(true) }

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Recommendation : Screen("recommendation", "Ulam", { Icon(Icons.Default.Restaurant, null) })
    object AddIngredient : Screen("add_ingredient", "Inventory", { Icon(Icons.Default.Add, null) })
    object AddRecipe : Screen("add_recipe", "Recipe", { Icon(Icons.AutoMirrored.Filled.List, null) })
    object Restock : Screen("restock", "Restock", { Icon(Icons.Default.NotificationsActive, null) })
    object ShoppingList : Screen("shopping_list", "Shopping", { Icon(Icons.Default.ShoppingCart, null) })
    
    // Non-tab screens
    object IngredientList : Screen("ingredient_list", "Full Inventory", { Icon(Icons.AutoMirrored.Filled.List, null) })
    object RecipeList : Screen("recipe_list", "All Recipes", { Icon(Icons.AutoMirrored.Filled.List, null) })
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val swipeEnabled = remember { mutableStateOf(true) }
    
    val tabItems = listOf(
        Screen.Recommendation,
        Screen.AddIngredient,
        Screen.AddRecipe,
        Screen.Restock,
        Screen.ShoppingList
    )
    
    val pagerState = rememberPagerState(pageCount = { tabItems.size })
    
    // We need to know if we are on a sub-screen to hide/disable pager or handle navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route ?: Screen.Recommendation.route
    val isSubScreen = currentNavRoute == Screen.IngredientList.route || currentNavRoute == Screen.RecipeList.route

    CompositionLocalProvider(LocalPagerSwipeEnabled provides swipeEnabled) {
        Scaffold(
            bottomBar = {
                if (!isSubScreen) {
                    NavigationBar {
                        tabItems.forEachIndexed { index, screen ->
                            NavigationBarItem(
                                icon = screen.icon,
                                label = { Text(screen.label) },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "main_tabs",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("main_tabs") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = swipeEnabled.value
                    ) { page ->
                        when (tabItems[page]) {
                            Screen.Recommendation -> RecommendationScreen()
                            Screen.AddIngredient -> IngredientInputScreen(
                                onExpandList = { navController.navigate(Screen.IngredientList.route) }
                            )
                            Screen.AddRecipe -> RecipeInputScreen(
                                onExpandList = { navController.navigate(Screen.RecipeList.route) }
                            )
                            Screen.Restock -> RestockScreen()
                            Screen.ShoppingList -> ShoppingListScreen()
                            else -> {} // Should not happen with tabItems
                        }
                    }
                }
                
                composable(Screen.IngredientList.route) {
                    IngredientListScreen(onBack = { navController.popBackStack() }) 
                }
                composable(Screen.RecipeList.route) { 
                    RecipeListScreen(onBack = { navController.popBackStack() }) 
                }
            }
        }
    }
}
