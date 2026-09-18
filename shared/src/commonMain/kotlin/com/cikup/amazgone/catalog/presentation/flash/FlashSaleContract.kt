package com.cikup.amazgone.catalog.presentation.flash

import com.cikup.amazgone.catalog.domain.model.FlashDeal
import com.cikup.amazgone.catalog.domain.model.FlashSale
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class FlashSaleState(
    val sale: FlashSale? = null,
    val now: Long = 0,
    val selectedCategory: String? = null,
    val savedIds: Set<String> = emptySet(),
) : UiState {
    val categories: List<String> get() = sale?.deals?.map { it.product.categorySlug }?.distinct().orEmpty()
    val visibleDeals: List<FlashDeal>
        get() = sale?.deals?.filter { selectedCategory == null || it.product.categorySlug == selectedCategory }.orEmpty()
}

sealed interface FlashSaleIntent : UiIntent {
    data class SelectCategory(val slug: String?) : FlashSaleIntent
    data class OpenProduct(val productId: String) : FlashSaleIntent
    data class ToggleSaved(val productId: String) : FlashSaleIntent
    data object Back : FlashSaleIntent
}

sealed interface FlashSaleEffect : UiEffect {
    data class NavigateToProduct(val productId: String, val origin: String) : FlashSaleEffect
    data object NavigateBack : FlashSaleEffect
}

const val FLASH_SCREEN_ORIGIN = "flash-screen"
