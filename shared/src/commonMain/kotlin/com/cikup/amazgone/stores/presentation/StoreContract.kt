package com.cikup.amazgone.stores.presentation

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.stores.domain.model.StoreSummary

data class StoreState(
    val storeId: String,
    val isLoading: Boolean = true,
    val summary: StoreSummary? = null,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val sort: SortOrder = SortOrder.RELEVANCE,
    val products: List<Product> = emptyList(),
) : UiState {
    val notFound: Boolean get() = !isLoading && summary == null
}

sealed interface StoreIntent : UiIntent {
    data class SelectCategory(val slug: String?) : StoreIntent
    data class ChangeSort(val sort: SortOrder) : StoreIntent
    data class OpenProduct(val productId: String) : StoreIntent
    data object Back : StoreIntent
}

sealed interface StoreEffect : UiEffect {
    data class NavigateToProduct(val productId: String, val origin: String) : StoreEffect
    data object NavigateBack : StoreEffect
}

/** Shared-element namespace for products opened from a store page. */
const val STORE_ORIGIN = "store"

/** Sort options offered on a store page (RELEVANCE = the store's own order, newest launches first). */
val STORE_SORTS = listOf(SortOrder.RELEVANCE, SortOrder.PRICE_LOW_TO_HIGH, SortOrder.PRICE_HIGH_TO_LOW, SortOrder.TOP_RATED)
