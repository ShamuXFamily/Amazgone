package com.cikup.amazgone.catalog.presentation.search

import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class SearchState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val sort: SortOrder = SortOrder.RELEVANCE,
    val results: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isFilterSheetOpen: Boolean = false,
) : UiState {
    val showNoResults: Boolean get() = !isLoading && results.isEmpty()
}

sealed interface SearchIntent : UiIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object ClearQuery : SearchIntent
    data class SelectCategory(val slug: String?) : SearchIntent
    data class ChangeSort(val sort: SortOrder) : SearchIntent
    data class ApplyFilters(val filters: SearchFilters) : SearchIntent
    data object OpenFilters : SearchIntent
    data object CloseFilters : SearchIntent
    data class OpenProduct(val productId: String) : SearchIntent
    data object Back : SearchIntent
}

sealed interface SearchEffect : UiEffect {
    data class NavigateToProduct(val productId: String, val origin: String) : SearchEffect
    data object NavigateBack : SearchEffect
}

const val SEARCH_ORIGIN = "search"
