package com.example.recipebook.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.recipebook.models.Meal
import com.example.recipebook.models.SearchFilter
import com.example.recipebook.ui.viewmodel.RecipeBookUiState
import com.example.recipebook.ui.widget.MealCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: RecipeBookUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSearch: () -> Unit,
    onMealClick: (Meal) -> Unit,
    onNavigateToFavourites: () -> Unit,
    onToggleFavourite: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск рецептов") },
                actions = {
                    IconButton(onClick = onNavigateToFavourites) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = "Избранное"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onSearchChange,
                    label = { Text("Название блюда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchFilter.entries.toTypedArray()) { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.title) }
                        )
                    }
                }
            }

            when {
                uiState.isSearchLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.searchError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                uiState.searchError,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                            Button(onClick = onSearch) {
                                Text("Повторить поиск")
                            }
                        }
                    }
                }

                uiState.searchResults.isNotEmpty() -> {
                    LazyColumn {
                        items(uiState.searchResults) { meal ->
                            MealCard(
                                meal = meal,
                                isFavourite = uiState.favourites.any { it.id == meal.id },
                                onClick = { onMealClick(meal) },
                                onToggleFavourite = { onToggleFavourite(meal.id) }
                            )
                        }
                    }
                }

                uiState.hasCompletedSearch -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "По запросу \"${uiState.query.trim()}\" ничего не найдено",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Введите запрос для поиска рецептов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
