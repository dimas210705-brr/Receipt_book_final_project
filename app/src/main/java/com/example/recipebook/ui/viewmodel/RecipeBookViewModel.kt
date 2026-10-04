package com.example.recipebook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipebook.api.MealRepository
import com.example.recipebook.data.local.FavouriteRepository
import com.example.recipebook.models.Meal
import com.example.recipebook.models.Recipe
import com.example.recipebook.models.SearchFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 400L

sealed interface SearchApiStatus {
    data object Idle : SearchApiStatus
    data object Loading : SearchApiStatus
    data class Success(val meals: List<Meal>) : SearchApiStatus
    data class Error(val message: String) : SearchApiStatus
}

sealed interface DetailApiStatus {
    data object Idle : DetailApiStatus
    data object Loading : DetailApiStatus
    data class Success(val recipe: Recipe) : DetailApiStatus
    data class Error(val message: String) : DetailApiStatus
}

data class RecipeBookUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val searchResults: List<Meal> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchError: String? = null,
    val hasCompletedSearch: Boolean = false,

    val selectedRecipe: Recipe? = null,
    val isRecipeLoading: Boolean = false,
    val recipeError: String? = null,

    val favourites: List<Meal> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeBookViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {

    // Источник 1: Ввод пользователя (StateFlow)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Источник 2: Пользовательский фильтр (StateFlow)
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // Источник 4: Поток событий retry/refresh (SharedFlow)
    private val _searchRetryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Потоки детального экрана
    private val _selectedRecipeId = MutableStateFlow<Int?>(null)
    private val _recipeRetryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Реактивная цепочка поиска: Query + Retry -> Debounce -> Distinct -> FlatMapLatest(Network)
    private val searchApiStatusFlow: Flow<SearchApiStatus> =
        combine(
            _searchQuery,
            _searchRetryTrigger.onStart { emit(Unit) }
        ) { query, _ -> query }
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    val trimmed = query.trim()
                    if (trimmed.isBlank()) {
                        emit(SearchApiStatus.Idle)
                        return@flow
                    }

                    emit(SearchApiStatus.Loading)
                    try {
                        val results = mealRepository.searchMeals(trimmed)
                        emit(SearchApiStatus.Success(results))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        emit(
                            SearchApiStatus.Error(
                                "Не удалось выполнить поиск. Проверьте подключение и повторите попытку."
                            )
                        )
                    }
                }
            }

    // Реактивная цепочка загрузки деталей рецепта
    private val detailApiStatusFlow: Flow<DetailApiStatus> =
        combine(
            _selectedRecipeId,
            _recipeRetryTrigger.onStart { emit(Unit) }
        ) { id, _ -> id }
            .flatMapLatest { mealId ->
                flow {
                    if (mealId == null) {
                        emit(DetailApiStatus.Idle)
                        return@flow
                    }

                    emit(DetailApiStatus.Loading)
                    try {
                        val recipe = mealRepository.getRecipe(mealId)
                        emit(DetailApiStatus.Success(recipe))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        emit(
                            DetailApiStatus.Error(
                                "Не удалось загрузить рецепт. Повторите попытку позже."
                            )
                        )
                    }
                }
            }

    // Источник 3 (Data layer Room): favouriteRepository.observeFavourites()
    // Финальная композиция всех потоков через combine
    val uiState: StateFlow<RecipeBookUiState> = combine(
        _searchQuery,
        _searchFilter,
        searchApiStatusFlow,
        favouriteRepository.observeFavourites(),
        detailApiStatusFlow
    ) { query, filter, searchStatus, favourites, detailStatus ->
        val rawMeals = (searchStatus as? SearchApiStatus.Success)?.meals ?: emptyList()

        val filteredMeals = when (filter) {
            SearchFilter.ALL -> rawMeals
            SearchFilter.ONLY_FAVOURITES -> {
                val favIds = favourites.map { it.id }.toSet()
                rawMeals.filter { it.id in favIds }
            }
            SearchFilter.ALPHABETICAL -> rawMeals.sortedBy { it.title }
        }

        RecipeBookUiState(
            query = query,
            filter = filter,
            searchResults = filteredMeals,
            isSearchLoading = searchStatus is SearchApiStatus.Loading,
            searchError = (searchStatus as? SearchApiStatus.Error)?.message,
            hasCompletedSearch = searchStatus is SearchApiStatus.Success,

            selectedRecipe = (detailStatus as? DetailApiStatus.Success)?.recipe,
            isRecipeLoading = detailStatus is DetailApiStatus.Loading,
            recipeError = (detailStatus as? DetailApiStatus.Error)?.message,

            favourites = favourites
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RecipeBookUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun searchMeals() {
        _searchRetryTrigger.tryEmit(Unit)
    }

    fun loadRecipe(mealId: Int) {
        _selectedRecipeId.value = mealId
    }

    fun retryLoadRecipe() {
        _recipeRetryTrigger.tryEmit(Unit)
    }

    fun clearSelection() {
        _selectedRecipeId.value = null
    }

    fun toggleFavourite(mealId: Int) {
        viewModelScope.launch {
            val isCurrentlyFavourite = uiState.value.favourites.any { it.id == mealId }
            if (isCurrentlyFavourite) {
                favouriteRepository.removeFromFavourites(mealId)
            } else {
                val meal = findMealById(mealId) ?: return@launch
                favouriteRepository.addToFavourites(meal)
            }
        }
    }

    private fun findMealById(mealId: Int): Meal? {
        uiState.value.searchResults.find { it.id == mealId }?.let { return it }

        val recipe = uiState.value.selectedRecipe
        if (recipe != null && recipe.id == mealId) {
            return Meal(id = recipe.id, title = recipe.title, image = recipe.image)
        }

        return uiState.value.favourites.find { it.id == mealId }
    }
}
