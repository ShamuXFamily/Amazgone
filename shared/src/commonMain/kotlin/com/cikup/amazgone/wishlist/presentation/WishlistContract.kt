package com.cikup.amazgone.wishlist.presentation

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class WishlistState(val products: List<Product> = emptyList(), val isLoading: Boolean = true) : UiState

sealed interface WishlistIntent : UiIntent {
    data class OpenProduct(val productId: String) : WishlistIntent
    data class Remove(val productId: String) : WishlistIntent
    data object Back : WishlistIntent
}

sealed interface WishlistEffect : UiEffect {
    data class NavigateToProduct(val productId: String, val origin: String) : WishlistEffect
    data object NavigateBack : WishlistEffect
}

const val WISHLIST_ORIGIN = "wishlist"
