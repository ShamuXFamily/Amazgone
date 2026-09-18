package com.cikup.amazgone.catalog.data.remote

import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId

/** One upstream product API. Implementations map their DTOs into the shared domain model. */
interface CatalogSource {
    val id: CatalogSourceId
    /** How long a full fetch stays fresh before the syncer refreshes it. */
    val ttlMillis: Long
    suspend fun fetchCatalog(): CatalogBatch
    suspend fun search(query: String): CatalogBatch
}
