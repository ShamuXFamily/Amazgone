package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import com.cikup.amazgone.games.domain.model.LightningDeal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveLightningDealUseCase(private val catalog: CatalogRepository) {
    operator fun invoke(ticks: Flow<Long>): Flow<LightningDeal?> =
        combine(catalog.observeDeals(CANDIDATES), ticks) { deals, now -> LightningDeal.forTime(deals, now) }
            .distinctUntilChanged { a, b -> a?.windowStart == b?.windowStart && a?.product?.id == b?.product?.id }

    private companion object {
        const val CANDIDATES = 24
    }
}
