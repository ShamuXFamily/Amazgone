package com.cikup.amazgone.stores.domain.usecase

import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.model.StoreSummary
import com.cikup.amazgone.stores.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Stores grouped for the directory: official first, then Amazgone, digital shops and other brands. */
class ObserveStoresUseCase(private val stores: StoreRepository) {
    operator fun invoke(): Flow<Map<StoreKind, List<StoreSummary>>> = stores.observeStores().map { all ->
        KIND_ORDER.associateWith { kind -> all.filter { it.store.kind == kind } }.filterValues { it.isNotEmpty() }
    }

    private companion object {
        val KIND_ORDER = listOf(StoreKind.OFFICIAL, StoreKind.AMAZGONE, StoreKind.DIGITAL, StoreKind.BRAND)
    }
}
