package com.cikup.amazgone.stores.domain.repository

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.stores.domain.model.StoreSummary
import kotlinx.coroutines.flow.Flow

/** Read-only view of the catalog grouped by seller (always from the local database). */
interface StoreRepository {
    fun observeStores(): Flow<List<StoreSummary>>
    fun observeProducts(storeId: String): Flow<List<Product>>
}
