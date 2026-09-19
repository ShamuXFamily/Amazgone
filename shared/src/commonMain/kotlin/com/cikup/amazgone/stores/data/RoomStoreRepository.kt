package com.cikup.amazgone.stores.data

import com.cikup.amazgone.catalog.data.local.CatalogDao
import com.cikup.amazgone.catalog.data.local.StoreRow
import com.cikup.amazgone.catalog.data.local.toDomain
import com.cikup.amazgone.stores.domain.model.Store
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.model.StoreSummary
import com.cikup.amazgone.stores.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStoreRepository(private val dao: CatalogDao) : StoreRepository {
    override fun observeStores(): Flow<List<StoreSummary>> = dao.observeStores().map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeProducts(storeId: String) = dao.observeStoreProducts(storeId).map { rows -> rows.map { it.toDomain() } }
}

private fun StoreRow.toDomain(): StoreSummary? {
    val kind = StoreKind.entries.firstOrNull { it.name == storeKind } ?: return null
    return StoreSummary(Store(storeId, storeName, kind), productCount, averageRating, topCategory)
}
