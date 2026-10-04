package com.example.recipebook.models

enum class SearchFilter(val title: String) {
    ALL("Все"),
    ONLY_FAVOURITES("Только в избранном"),
    ALPHABETICAL("По алфавиту")
}
