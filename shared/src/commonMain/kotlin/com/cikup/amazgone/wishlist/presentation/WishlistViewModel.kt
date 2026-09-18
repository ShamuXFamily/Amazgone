package com.cikup.amazgone.wishlist.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase

class WishlistViewModel(observeWishlist: ObserveWishlistUseCase, private val toggle: ToggleWishlistUseCase) :
    MviViewModel<WishlistState, WishlistIntent, WishlistEffect>(WishlistState()) {

    init {
        observeWishlist().observe { setState { copy(products = it, isLoading = false) } }
    }

    override fun handleIntent(intent: WishlistIntent) {
        when (intent) {
            is WishlistIntent.OpenProduct -> sendEffect(WishlistEffect.NavigateToProduct(intent.productId, WISHLIST_ORIGIN))
            is WishlistIntent.Remove -> launchSafely { toggle(intent.productId) }
            WishlistIntent.Back -> sendEffect(WishlistEffect.NavigateBack)
        }
    }
}
