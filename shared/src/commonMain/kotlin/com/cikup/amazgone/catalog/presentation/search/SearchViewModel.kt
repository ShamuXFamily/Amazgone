package com.cikup.amazgone.catalog.presentation.search

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.catalog.domain.usecase.ObserveCategoriesUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchProductsUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchRemoteCatalogUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    initialCategory: String?,
    searchProducts: SearchProductsUseCase,
    private val searchRemote: SearchRemoteCatalogUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val analytics: Analytics,
) : MviViewModel<SearchState, SearchIntent, SearchEffect>(SearchState()) {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters(categorySlug = initialCategory))
    private val sort = MutableStateFlow(SortOrder.RELEVANCE)

    init {
        setState { copy(filters = SearchFilters(categorySlug = initialCategory)) }
        val debouncedQuery = query.debounce(LOCAL_DEBOUNCE_MS).distinctUntilChanged()
        combine(debouncedQuery, filters, sort, ::Triple)
            .flatMapLatest { (q, f, s) -> searchProducts(q, f, s) }
            .observe { results -> setState { copy(results = results, isLoading = false) } }

        launchSafely {
            query.debounce(REMOTE_DEBOUNCE_MS).distinctUntilChanged().collectLatest {
                analytics.search(it) // once the user pauses typing, not per keystroke
                searchRemote(it)
            }
        }
        observeCategories().observe { setState { copy(categories = it) } }
    }

    override fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> updateQuery(intent.query)
            SearchIntent.ClearQuery -> updateQuery("")
            is SearchIntent.SelectCategory -> applyFilters(currentState.filters.copy(categorySlug = intent.slug))
            is SearchIntent.ChangeSort -> {
                sort.value = intent.sort
                setState { copy(sort = intent.sort) }
            }
            is SearchIntent.ApplyFilters -> {
                applyFilters(intent.filters)
                setState { copy(isFilterSheetOpen = false) }
            }
            SearchIntent.OpenFilters -> setState { copy(isFilterSheetOpen = true) }
            SearchIntent.CloseFilters -> setState { copy(isFilterSheetOpen = false) }
            is SearchIntent.OpenProduct -> sendEffect(SearchEffect.NavigateToProduct(intent.productId, SEARCH_ORIGIN))
            SearchIntent.Back -> sendEffect(SearchEffect.NavigateBack)
        }
    }

    private fun updateQuery(value: String) {
        query.value = value
        setState { copy(query = value) }
    }

    private fun applyFilters(value: SearchFilters) {
        filters.value = value
        setState { copy(filters = value) }
    }

    private companion object {
        const val LOCAL_DEBOUNCE_MS = 250L
        const val REMOTE_DEBOUNCE_MS = 700L
    }
}
