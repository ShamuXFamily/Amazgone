package com.cikup.amazgone.cart.presentation

import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.cart.domain.usecase.RestoreCartUseCase
import com.cikup.amazgone.cart.domain.usecase.UpdateCartQuantityUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel

class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartQuantityUseCase,
    private val restoreCart: RestoreCartUseCase,
) : MviViewModel<CartState, CartIntent, CartEffect>(CartState()) {

    init {
        observeCart().observe { setState { copy(summary = it, isLoading = false) } }
    }

    override fun handleIntent(intent: CartIntent) {
        when (intent) {
            is CartIntent.ChangeQuantity -> launchSafely {
                if (intent.quantity <= 0) remove(intent.productId) else updateQuantity(intent.productId, intent.quantity)
            }
            is CartIntent.Remove -> launchSafely { remove(intent.productId) }
            is CartIntent.Undo -> launchSafely { restoreCart(listOf(intent.entry)) }
            is CartIntent.OpenProduct -> sendEffect(CartEffect.NavigateToProduct(intent.productId, CART_ORIGIN))
            CartIntent.Checkout -> if (!currentState.summary.isEmpty) sendEffect(CartEffect.NavigateToCheckout)
            CartIntent.BrowseDeals -> sendEffect(CartEffect.NavigateHome)
        }
    }

    private suspend fun remove(productId: String) {
        val line = currentState.summary.lines.firstOrNull { it.product.id == productId } ?: return
        updateQuantity(productId, 0)
        sendEffect(CartEffect.ShowUndo(CartEntry(productId, line.quantity, 0), line.product.title))
    }
}
