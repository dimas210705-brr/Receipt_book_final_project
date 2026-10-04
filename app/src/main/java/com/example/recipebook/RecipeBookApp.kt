package com.example.recipebook

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recipebook.ui.screen.CollectionsScreen
import com.example.recipebook.ui.screen.FavouritesScreen
import com.example.recipebook.ui.screen.HistoryScreen
import com.example.recipebook.ui.screen.RecipeDetailScreen
import com.example.recipebook.ui.screen.SearchScreen
import com.example.recipebook.ui.screen.SettingsScreen
import com.example.recipebook.ui.viewmodel.FinalAppViewModel
import com.example.recipebook.ui.viewmodel.RecipeBookViewModel

sealed class Screen(val route: String, val title: String = "") {
    data object Search : Screen("search", "Поиск")
    data object Favourites : Screen("favourites", "Избранное")
    data object Collections : Screen("collections", "Кукбук")
    data object History : Screen("history", "История")
    data object Settings : Screen("settings", "Настройки")
    data object Detail : Screen("detail/{mealId}") {
        fun createRoute(mealId: Int): String = "detail/$mealId"
    }
}

@Composable
fun RecipeBookApp() {
    val searchViewModel: RecipeBookViewModel = hiltViewModel()
    val finalViewModel: FinalAppViewModel = hiltViewModel()
    val navController: NavHostController = rememberNavController()

    val uiState by searchViewModel.uiState.collectAsState()
    val history by finalViewModel.historyFlow.collectAsState(initial = emptyList())
    val collections by finalViewModel.collectionsFlow.collectAsState(initial = emptyList())
    val settings by finalViewModel.settingsFlow.collectAsState()
    val currentNote by finalViewModel.currentNoteFlow.collectAsState(initial = null)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf(Screen.Search.route, Screen.Favourites.route, Screen.Collections.route, Screen.History.route, Screen.Settings.route)) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Search.route,
                        onClick = { navController.navigate(Screen.Search.route) },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Поиск") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Favourites.route,
                        onClick = { navController.navigate(Screen.Favourites.route) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Избранное") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Collections.route,
                        onClick = { navController.navigate(Screen.Collections.route) },
                        icon = { Icon(Icons.Default.Collections, contentDescription = null) },
                        label = { Text("Кукбук") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = { navController.navigate(Screen.History.route) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("История") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Опции") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(
                    uiState = uiState,
                    onSearchChange = searchViewModel::updateSearchQuery,
                    onFilterChange = searchViewModel::updateFilter,
                    onSearch = searchViewModel::searchMeals,
                    onMealClick = { meal -> navController.navigate(Screen.Detail.createRoute(meal.id)) },
                    onNavigateToFavourites = { navController.navigate(Screen.Favourites.route) },
                    onToggleFavourite = searchViewModel::toggleFavourite
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("mealId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mealId = backStackEntry.arguments?.getInt("mealId") ?: return@composable
                LaunchedEffect(mealId) {
                    searchViewModel.loadRecipe(mealId)
                    finalViewModel.recordRecipeView(mealId, uiState.selectedRecipe?.title ?: "", uiState.selectedRecipe?.image ?: "")
                    finalViewModel.selectRecipeForNote(mealId)
                }

                RecipeDetailScreen(
                    uiState = uiState,
                    note = currentNote,
                    onBackClick = {
                        searchViewModel.clearSelection()
                        navController.popBackStack()
                    },
                    onRetry = searchViewModel::retryLoadRecipe,
                    onToggleFavourite = { searchViewModel.toggleFavourite(mealId) },
                    onSaveNote = { text, rating, isCooked -> finalViewModel.saveNote(mealId, text, rating, isCooked) }
                )
            }

            composable(Screen.Favourites.route) {
                FavouritesScreen(
                    uiState = uiState,
                    onMealClick = { meal -> navController.navigate(Screen.Detail.createRoute(meal.id)) },
                    onToggleFavourite = searchViewModel::toggleFavourite,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Collections.route) {
                CollectionsScreen(
                    collections = collections,
                    onCreateCollection = finalViewModel::createCollection,
                    onDeleteCollection = finalViewModel::deleteCollection,
                    onSelectCollection = { }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    history = history,
                    favourites = uiState.favourites,
                    onMealClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onToggleFavourite = searchViewModel::toggleFavourite,
                    onClearHistory = finalViewModel::clearHistory
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onAutoCacheChange = finalViewModel::setAutoCache,
                    onHistoryChange = finalViewModel::setHistoryEnabled,
                    onClearCacheClick = finalViewModel::clearCache
                )
            }
        }
    }
}
