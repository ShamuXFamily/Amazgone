package com.cikup.amazgone.wishlist.presentation

import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistUseCase
import com.cikup.amazgone.wishlist.domain.usecase.SaveToWishlistUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase

class WishlistViewModel(
    observeWishlist: ObserveWishlistUseCase,
    observeCart: ObserveCartUseCase,
    time: TimeProvider,
    private val toggle: ToggleWishlistUseCase,
    private val save: SaveToWishlistUseCase,
    private val addToCart: AddToCartUseCase,
    private val analytics: Analytics,
) : MviViewModel<WishlistState, WishlistIntent, WishlistEffect>(WishlistState(now = time.nowMillis())) {

    init {
        observeWishlist().observe { setState { copy(products = it, isLoading = false) } }
        observeCart().observe { cart -> setState { copy(inCart = cart.lines.map { it.product.id }.toSet()) } }
    }

    override fun handleIntent(intent: WishlistIntent) {
        when (intent) {
            is WishlistIntent.OpenProduct -> sendEffect(WishlistEffect.NavigateToProduct(intent.productId, WISHLIST_ORIGIN))
            is WishlistIntent.AddToCart -> launchSafely { if (add(intent.productId)) sendEffect(WishlistEffect.AddedToCart(1)) }
            WishlistIntent.AddAllToCart -> launchSafely {
                val added = currentState.addable.count { add(it.id) }
                if (added > 0) sendEffect(WishlistEffect.AddedToCart(added))
            }
            is WishlistIntent.Remove -> remove(intent.productId)
            is WishlistIntent.Undo -> launchSafely { save(intent.productId) }
            is WishlistIntent.SetFilter -> setState { copy(filter = intent.filter) }
            is WishlistIntent.SetSort -> setState { copy(sort = intent.sort) }
            WishlistIntent.Browse -> sendEffect(WishlistEffect.NavigateHome)
            WishlistIntent.Back -> sendEffect(WishlistEffect.NavigateBack)
        }
    }

    private suspend fun add(productId: String): Boolean {
        val product = currentState.products.firstOrNull { it.id == productId } ?: return false
        if (!product.isInStock || addToCart(productId) !is DomainResult.Success) return false
        analytics.addToCart(product, 1)
        return true
    }

    private fun remove(productId: String) {
        val product = currentState.products.firstOrNull { it.id == productId } ?: return
        launchSafely {
            toggle(productId)
            sendEffect(WishlistEffect.ShowUndo(productId, product.title))
        }
    }
}
