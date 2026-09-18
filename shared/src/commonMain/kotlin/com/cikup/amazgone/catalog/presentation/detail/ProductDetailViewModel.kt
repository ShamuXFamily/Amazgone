package com.cikup.amazgone.catalog.presentation.detail

import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveQuantityInCartUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveProductDetailUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveRecommendationsUseCase
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.wishlist.domain.usecase.ObserveIsSavedUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModel(
    productId: String,
    origin: String,
    observeDetail: ObserveProductDetailUseCase,
    observeRecommendations: ObserveRecommendationsUseCase,
    private val addToCart: AddToCartUseCase,
    observeQuantityInCart: ObserveQuantityInCartUseCase,
    observeIsSaved: ObserveIsSavedUseCase,
    private val toggleWishlist: ToggleWishlistUseCase,
    time: TimeProvider,
) : MviViewModel<ProductDetailState, ProductDetailIntent, ProductDetailEffect>(ProductDetailState(productId, origin)) {

    init {
        setState { copy(now = time.nowMillis()) }
        val detail = observeDetail(productId)
        detail.observe { found ->
            setState { copy(isLoading = false, product = found?.product, reviews = found?.reviews.orEmpty()) }
        }

        detail.filterNotNull()
            .distinctUntilChangedBy { it.product.categorySlug }
            .flatMapLatest { observeRecommendations(it.product) }
            .observe { setState { copy(recommendations = it) } }

        observeQuantityInCart(productId)
            .observe { setState { copy(quantityInCart = it) } }
        observeIsSaved(productId).observe { setState { copy(isSaved = it) } }
    }

    override fun handleIntent(intent: ProductDetailIntent) {
        when (intent) {
            ProductDetailIntent.AddToCart -> add()
            is ProductDetailIntent.ChangeQuantity -> setState { copy(quantity = intent.quantity.coerceIn(1, CartCalculator.MAX_QUANTITY_PER_ITEM)) }
            ProductDetailIntent.ToggleWishlist -> launchSafely { toggleWishlist(currentState.productId) }
            is ProductDetailIntent.OpenProduct ->
                sendEffect(ProductDetailEffect.NavigateToProduct(intent.productId, RECOMMENDATION_ORIGIN))
            ProductDetailIntent.Back -> sendEffect(ProductDetailEffect.NavigateBack)
        }
    }

    private fun add() {
        val product = currentState.product ?: return
        if (currentState.isAdding || !product.isInStock) return
        setState { copy(isAdding = true) }
        launchSafely {
            when (addToCart(product.id, currentState.quantity)) {
                is DomainResult.Success -> {
                    sendEffect(ProductDetailEffect.FlyToCart(product.id, product.thumbnailUrl))
                    setState { copy(isAdding = false, justAdded = true, quantity = 1) }
                    delay(ADDED_FEEDBACK_MS)
                    setState { copy(justAdded = false) }
                }
                is DomainResult.Failure -> {
                    sendEffect(ProductDetailEffect.LimitReached)
                    setState { copy(isAdding = false) }
                }
            }
        }
    }

    private companion object {
        const val ADDED_FEEDBACK_MS = 1_400L
    }
}
