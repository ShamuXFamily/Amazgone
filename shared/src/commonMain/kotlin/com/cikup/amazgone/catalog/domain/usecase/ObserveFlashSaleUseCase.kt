package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.FlashSale
import com.cikup.amazgone.catalog.domain.model.FlashSaleClock
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Current flash window over the biggest discounts, re-evaluated on each [ticks] emission. */
class ObserveFlashSaleUseCase(private val catalog: CatalogRepository) {
    operator fun invoke(ticks: Flow<Long>, limit: Int = DEFAULT_LIMIT): Flow<FlashSale> =
        combine(catalog.observeDeals(limit), ticks) { deals, now -> FlashSaleClock.sale(deals, now) }

    private companion object {
        const val DEFAULT_LIMIT = 24
    }
}
