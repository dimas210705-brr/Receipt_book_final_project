package com.example.recipebook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebook.data.local.CollectionsDao
import com.example.recipebook.data.local.HistoryDao
import com.example.recipebook.data.local.RecipeCollectionEntity
import com.example.recipebook.data.local.RecipeHistoryEntity
import com.example.recipebook.data.local.RecipeNoteEntity
import com.example.recipebook.data.preferences.UserSettings
import com.example.recipebook.data.preferences.UserSettingsRepository
import com.example.recipebook.data.repository.OfflineFirstRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinalAppViewModel @Inject constructor(
    private val historyDao: HistoryDao,
    private val collectionsDao: CollectionsDao,
    private val userSettingsRepository: UserSettingsRepository,
    private val offlineRecipeRepository: OfflineFirstRecipeRepository
) : ViewModel() {

    val historyFlow: Flow<List<RecipeHistoryEntity>> = historyDao.observeHistory()
    val collectionsFlow: Flow<List<RecipeCollectionEntity>> = collectionsDao.observeCollections()

    val settingsFlow: StateFlow<UserSettings> = userSettingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _selectedMealId = MutableStateFlow<Int?>(null)
    val currentNoteFlow: Flow<RecipeNoteEntity?> = _selectedMealId.flatMapLatest { id ->
        if (id == null) flowOf(null) else collectionsDao.observeNoteForRecipe(id)
    }

    fun selectRecipeForNote(mealId: Int) {
        _selectedMealId.value = mealId
    }

    fun recordRecipeView(recipeId: Int, title: String, image: String) {
        if (!settingsFlow.value.isHistoryEnabled) return
        viewModelScope.launch {
            historyDao.recordView(RecipeHistoryEntity(recipeId = recipeId, title = title, image = image))
        }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearHistory() }
    }

    fun createCollection(title: String, desc: String) {
        viewModelScope.launch {
            collectionsDao.insertCollection(RecipeCollectionEntity(title = title, description = desc))
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch { collectionsDao.deleteCollection(collectionId) }
    }

    fun saveNote(recipeId: Int, noteText: String, rating: Int, isCooked: Boolean) {
        viewModelScope.launch {
            collectionsDao.saveNote(RecipeNoteEntity(recipeId = recipeId, noteText = noteText, rating = rating, isCooked = isCooked))
        }
    }

    fun setAutoCache(enabled: Boolean) = viewModelScope.launch { userSettingsRepository.setAutoCache(enabled) }
    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch { userSettingsRepository.setHistoryEnabled(enabled) }
    fun clearCache() = viewModelScope.launch { offlineRecipeRepository.clearCache() }
}
