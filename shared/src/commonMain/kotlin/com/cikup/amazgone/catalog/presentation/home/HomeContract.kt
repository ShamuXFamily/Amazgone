package com.cikup.amazgone.catalog.presentation.home

import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.FlashSale
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.core.sync.domain.SyncStatus
import com.cikup.amazgone.stores.domain.model.StoreSummary

data class HomeState(
    val isLoading: Boolean = true,
    val deals: List<Product> = emptyList(),
    val topRated: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val officialStores: List<StoreSummary> = emptyList(),
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedCategory: String? = null,
    val flashSale: FlashSale? = null,
    val now: Long = 0,
    val savedIds: Set<String> = emptySet(),
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val isRefreshing: Boolean = false,
) : UiState {
    val isEmpty: Boolean get() = !isLoading && products.isEmpty() && deals.isEmpty()
}

/** Shortcuts from the home screen to other parts of the app. */
enum class HomeDestination { SEARCH, CATEGORIES, WALLET, SPIN, SCRATCH, ORDERS, FLASH_SALE, STORES }

sealed interface HomeIntent : UiIntent {
    data class SelectCategory(val slug: String?) : HomeIntent
    data object Refresh : HomeIntent
    data class Open(val destination: HomeDestination) : HomeIntent
    data class OpenProduct(val productId: String, val origin: String) : HomeIntent
    data class ToggleSaved(val productId: String) : HomeIntent
    data class OpenStore(val storeId: String) : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class Navigate(val destination: HomeDestination) : HomeEffect
    data class NavigateToProduct(val productId: String, val origin: String) : HomeEffect
    data class NavigateToStore(val storeId: String) : HomeEffect
}
