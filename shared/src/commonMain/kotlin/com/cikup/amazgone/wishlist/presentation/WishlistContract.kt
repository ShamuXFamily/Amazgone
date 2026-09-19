package com.cikup.amazgone.wishlist.presentation

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

enum class WishlistFilter { ALL, ON_SALE, IN_STOCK }

/** RECENT keeps the repository order (most recently saved first). */
enum class WishlistSort { RECENT, PRICE_LOW, PRICE_HIGH, DISCOUNT }

data class WishlistState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val filter: WishlistFilter = WishlistFilter.ALL,
    val sort: WishlistSort = WishlistSort.RECENT,
    /** Product ids already in the cart ("In cart ✓"). */
    val inCart: Set<String> = emptySet(),
    /** Clock for pre-order labels; 0 until read. */
    val now: Long = 0,
) : UiState {
    val visible: List<Product>
        get() = products.filter { product ->
            when (filter) {
                WishlistFilter.ALL -> true
                WishlistFilter.ON_SALE -> product.discountPercent > 0
                WishlistFilter.IN_STOCK -> product.isInStock
            }
        }.let { list ->
            when (sort) {
                WishlistSort.RECENT -> list
                WishlistSort.PRICE_LOW -> list.sortedBy { it.priceCoins }
                WishlistSort.PRICE_HIGH -> list.sortedByDescending { it.priceCoins }
                WishlistSort.DISCOUNT -> list.sortedByDescending { it.discountPercent }
            }
        }
    val totalCoins: Long get() = products.sumOf { it.priceCoins }
    val onSaleCount: Int get() = products.count { it.discountPercent > 0 }
    val savingsCoins: Long get() = products.sumOf { (it.originalPriceCoins ?: it.priceCoins) - it.priceCoins }
    /** Items "Add all to cart" would add: in stock and not already in the cart. */
    val addable: List<Product> get() = products.filter { it.isInStock && it.id !in inCart }
    val availableCount: Int get() = products.count { it.isInStock }
}

sealed interface WishlistIntent : UiIntent {
    data class OpenProduct(val productId: String) : WishlistIntent
    data class AddToCart(val productId: String) : WishlistIntent
    data object AddAllToCart : WishlistIntent
    data class Remove(val productId: String) : WishlistIntent
    data class Undo(val productId: String) : WishlistIntent
    data class SetFilter(val filter: WishlistFilter) : WishlistIntent
    data class SetSort(val sort: WishlistSort) : WishlistIntent
    data object Browse : WishlistIntent
    data object Back : WishlistIntent
}

sealed interface WishlistEffect : UiEffect {
    data class NavigateToProduct(val productId: String, val origin: String) : WishlistEffect
    data class AddedToCart(val count: Int) : WishlistEffect
    data class ShowUndo(val productId: String, val title: String) : WishlistEffect
    data object NavigateHome : WishlistEffect
    data object NavigateBack : WishlistEffect
}

const val WISHLIST_ORIGIN = "wishlist"
