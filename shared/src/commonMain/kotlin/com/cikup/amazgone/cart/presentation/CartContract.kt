package com.cikup.amazgone.cart.presentation

import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class CartState(
    val summary: CartSummary = CartSummary.EMPTY,
    val isLoading: Boolean = true,
    /** Spendable coins; null until the wallet has been read. */
    val balanceCoins: Long? = null,
) : UiState {
    /** Coins still missing to pay for the cart (0 when the wallet covers it). */
    val shortfallCoins: Long get() = balanceCoins?.let { (summary.totalCoins - it).coerceAtLeast(0) } ?: 0
}

sealed interface CartIntent : UiIntent {
    data class ChangeQuantity(val productId: String, val quantity: Int) : CartIntent
    data class Remove(val productId: String) : CartIntent
    data class Undo(val entry: CartEntry) : CartIntent
    data class OpenProduct(val productId: String) : CartIntent
    data class SaveForLater(val productId: String) : CartIntent
    data object PlayForCoins : CartIntent
    data object Checkout : CartIntent
    data object BrowseDeals : CartIntent
}

sealed interface CartEffect : UiEffect {
    data class ShowUndo(val entry: CartEntry, val title: String) : CartEffect
    data class NavigateToProduct(val productId: String, val origin: String) : CartEffect
    data object NavigateToCheckout : CartEffect
    data class SavedForLater(val title: String) : CartEffect
    data object NavigateToGames : CartEffect
    data object NavigateHome : CartEffect
}

const val CART_ORIGIN = "cart"
