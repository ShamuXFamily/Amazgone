package com.cikup.amazgone.catalog.presentation.detail

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.Review
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class ProductDetailState(
    val productId: String,
    val origin: String,
    val product: Product? = null,
    val reviews: List<Review> = emptyList(),
    val recommendations: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val quantityInCart: Int = 0,
    val isAdding: Boolean = false,
    val isSaved: Boolean = false,
    val quantity: Int = 1,
    /** Briefly true after a successful add so the button can morph to a check. */
    val justAdded: Boolean = false,
    /** Clock reading used for time-based labels (pre-order); 0 until read. */
    val now: Long = 0,
) : UiState {
    val notFound: Boolean get() = !isLoading && product == null
    val isPreorder: Boolean get() = now > 0 && product?.isPreorderAt(now) == true
}

sealed interface ProductDetailIntent : UiIntent {
    data object AddToCart : ProductDetailIntent
    data object ToggleWishlist : ProductDetailIntent
    data class ChangeQuantity(val quantity: Int) : ProductDetailIntent
    data class OpenProduct(val productId: String) : ProductDetailIntent
    data object Back : ProductDetailIntent
}

sealed interface ProductDetailEffect : UiEffect {
    data object NavigateBack : ProductDetailEffect
    data class NavigateToProduct(val productId: String, val origin: String) : ProductDetailEffect
    /** Starts the fly-to-cart animation from the add button towards the cart tab. */
    data class FlyToCart(val productId: String, val imageUrl: String) : ProductDetailEffect
    data object LimitReached : ProductDetailEffect
}

const val RECOMMENDATION_ORIGIN = "recommendation"
