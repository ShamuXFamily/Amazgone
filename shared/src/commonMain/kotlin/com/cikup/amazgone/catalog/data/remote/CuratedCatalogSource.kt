package com.cikup.amazgone.catalog.data.remote

import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.mapper.toCuratedBatch
import com.cikup.amazgone.catalog.data.mapper.toCuratedDtoOrNull
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.remote.FirebaseServices

/**
 * New arrivals edited in the Firebase console (`catalog` collection, public read, no client writes).
 * Read without sign-in so guests see them too. Without Firebase config it simply returns nothing.
 */
class CuratedCatalogSource(
    private val firebase: FirebaseServices,
    private val time: TimeProvider,
) : CatalogSource {
    override val id = CatalogSourceId.AMAZGONE
    override val ttlMillis = 60 * 60 * 1_000L

    override suspend fun fetchCatalog(): CatalogBatch {
        val firestore = firebase.firestore ?: return CatalogBatch.EMPTY
        return firestore.list(COLLECTION, authenticated = false)
            .mapNotNull { it.toCuratedDtoOrNull() }
            .toCuratedBatch(time.nowMillis())
    }

    /** The curated set is small and fully synced, so local full-text search already covers it. */
    override suspend fun search(query: String): CatalogBatch = CatalogBatch.EMPTY

    companion object {
        const val COLLECTION = "catalog"
    }
}
