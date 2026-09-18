package com.cikup.amazgone.catalog.presentation.flash

import com.cikup.amazgone.catalog.domain.usecase.ObserveFlashSaleUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistIdsUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class FlashSaleViewModel(
    ticker: TickerUseCase,
    observeFlashSale: ObserveFlashSaleUseCase,
    observeWishlistIds: ObserveWishlistIdsUseCase,
    private val toggleWishlist: ToggleWishlistUseCase,
) : MviViewModel<FlashSaleState, FlashSaleIntent, FlashSaleEffect>(FlashSaleState()) {

    init {
        val ticks = ticker().shareIn(vmScope, SharingStarted.WhileSubscribed(), replay = 1)
        ticks.observe { setState { copy(now = it) } }
        observeFlashSale(ticks).observe { setState { copy(sale = it) } }
        observeWishlistIds().observe { setState { copy(savedIds = it) } }
    }

    override fun handleIntent(intent: FlashSaleIntent) {
        when (intent) {
            is FlashSaleIntent.SelectCategory -> setState { copy(selectedCategory = intent.slug) }
            is FlashSaleIntent.OpenProduct -> sendEffect(FlashSaleEffect.NavigateToProduct(intent.productId, FLASH_SCREEN_ORIGIN))
            is FlashSaleIntent.ToggleSaved -> launchSafely { toggleWishlist(intent.productId) }
            FlashSaleIntent.Back -> sendEffect(FlashSaleEffect.NavigateBack)
        }
    }
}
