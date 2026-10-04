package com.example.recipebook.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recipebook.data.local.RecipeNoteEntity
import com.example.recipebook.ui.viewmodel.RecipeBookUiState
import com.example.recipebook.ui.widget.RecipeContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    uiState: RecipeBookUiState,
    note: RecipeNoteEntity?,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavourite: () -> Unit,
    onSaveNote: (String, Int, Boolean) -> Unit
) {
    var noteText by remember(note?.noteText) { mutableStateOf(note?.noteText ?: "") }
    var rating by remember(note?.rating) { mutableStateOf(note?.rating ?: 0) }
    var isCooked by remember(note?.isCooked) { mutableStateOf(note?.isCooked ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedRecipe?.title ?: "Рецепт", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    uiState.selectedRecipe?.let { recipe ->
                        val isFav = uiState.favourites.any { it.id == recipe.id }
                        IconButton(onClick = onToggleFavourite) {
                            Icon(Icons.Default.Favorite, contentDescription = "В избранное", tint = if (isFav) Color.Red else Color.Gray)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isRecipeLoading && uiState.selectedRecipe == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.recipeError != null && uiState.selectedRecipe == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.recipeError, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRetry) { Text("Повторить") }
                            OutlinedButton(onClick = onBackClick) { Text("Назад") }
                        }
                    }
                }
            }
            uiState.selectedRecipe != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Офлайн-баннер
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Офлайн-режим: рецепт сохранён в локальном кэше", style = MaterialTheme.typography.bodySmall)
                    }

                    // Личная заметка и статус
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Моя заметка к рецепту", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = {
                                    noteText = it
                                    onSaveNote(noteText, rating, isCooked)
                                },
                                label = { Text("Заметка (секреты, пропорции)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        IconButton(onClick = {
                                            rating = star
                                            onSaveNote(noteText, rating, isCooked)
                                        }) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = if (star <= rating) Color(0xFFFFB300) else Color.LightGray)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isCooked, onCheckedChange = {
                                        isCooked = it
                                        onSaveNote(noteText, rating, isCooked)
                                    })
                                    Text("Приготовлено", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    RecipeContent(recipe = uiState.selectedRecipe, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
