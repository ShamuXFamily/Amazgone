package com.cikup.amazgone.cart.presentation

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.cart.domain.usecase.RestoreCartUseCase
import com.cikup.amazgone.cart.domain.usecase.UpdateCartQuantityUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase
import com.cikup.amazgone.wishlist.domain.usecase.SaveToWishlistUseCase

class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartQuantityUseCase,
    private val restoreCart: RestoreCartUseCase,
    observeWallet: ObserveWalletUseCase,
    private val saveToWishlist: SaveToWishlistUseCase,
    private val analytics: Analytics,
) : MviViewModel<CartState, CartIntent, CartEffect>(CartState()) {

    init {
        observeCart().observe { setState { copy(summary = it, isLoading = false) } }
        observeWallet(recentLimit = 1).observe { setState { copy(balanceCoins = it.coins) } }
    }

    override fun handleIntent(intent: CartIntent) {
        when (intent) {
            is CartIntent.ChangeQuantity -> launchSafely {
                if (intent.quantity <= 0) remove(intent.productId) else updateQuantity(intent.productId, intent.quantity)
            }
            is CartIntent.Remove -> launchSafely { remove(intent.productId) }
            is CartIntent.SaveForLater -> launchSafely { saveForLater(intent.productId) }
            CartIntent.PlayForCoins -> sendEffect(CartEffect.NavigateToGames)
            is CartIntent.Undo -> launchSafely { restoreCart(listOf(intent.entry)) }
            is CartIntent.OpenProduct -> sendEffect(CartEffect.NavigateToProduct(intent.productId, CART_ORIGIN))
            CartIntent.Checkout -> if (!currentState.summary.isEmpty) sendEffect(CartEffect.NavigateToCheckout)
            CartIntent.BrowseDeals -> sendEffect(CartEffect.NavigateHome)
        }
    }

    private suspend fun saveForLater(productId: String) {
        val line = currentState.summary.lines.firstOrNull { it.product.id == productId } ?: return
        saveToWishlist(productId)
        analytics.addToWishlist(productId)
        updateQuantity(productId, 0)
        sendEffect(CartEffect.SavedForLater(line.product.title))
    }

    private suspend fun remove(productId: String) {
        val line = currentState.summary.lines.firstOrNull { it.product.id == productId } ?: return
        updateQuantity(productId, 0)
        analytics.removeFromCart(line.product, line.quantity)
        sendEffect(CartEffect.ShowUndo(CartEntry(productId, line.quantity, 0), line.product.title))
    }
}
