package com.cikup.amazgone.catalog.data.sync

import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.mapper.toBatch
import com.cikup.amazgone.catalog.data.remote.dto.SeedCatalogDto
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.network.AppJson

/**
 * First launch without network: fills Room from the bundled snapshot so the shop is browsable
 * immediately. Seed timestamps are 0, so the first online sync always refreshes them.
 */
class SeedCatalogImporter(
    private val repository: CatalogRepositoryImpl,
    private val readSeed: suspend () -> ByteArray,
) : StartupTask {
    override val name = "seed-catalog"

    override suspend fun run() {
        if (!repository.isEmpty()) return
        repository.save(parseSeed(readSeed().decodeToString()))
    }

    companion object {
        const val SEED_PATH = "files/seed/catalog.json"
        private const val SEED_TIMESTAMP = 0L

        fun parseSeed(json: String): CatalogBatch {
            val seed = AppJson.decodeFromString<SeedCatalogDto>(json)
            val dummy = seed.dummyjson.products.toBatch(SEED_TIMESTAMP)
            val games = seed.cheapshark.toBatch(SEED_TIMESTAMP)
            return CatalogBatch(dummy.products + games.products, dummy.reviews + games.reviews)
        }
    }
}
