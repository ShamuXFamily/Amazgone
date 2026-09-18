package com.cikup.amazgone.catalog.data.sync

import com.cikup.amazgone.catalog.data.remote.CatalogSource
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.sync.data.SyncMetaDao
import com.cikup.amazgone.core.sync.data.SyncMetaEntity
import com.cikup.amazgone.core.sync.domain.RemotePuller
import kotlinx.coroutines.CancellationException

/** Stale-while-revalidate: refreshes each source into Room once its TTL has expired. */
class CatalogSyncer(
    private val sources: List<CatalogSource>,
    private val repository: CatalogRepositoryImpl,
    private val syncMeta: SyncMetaDao,
    private val time: TimeProvider,
    private val logger: AppLogger,
) : RemotePuller {
    override val name = "catalog"
    override val requiresAuth = false

    override suspend fun pull(force: Boolean) {
        sources.forEach { source -> refresh(source, force) }
    }

    private suspend fun refresh(source: CatalogSource, force: Boolean) {
        val key = metaKey(source)
        val last = syncMeta.lastSyncedAt(key) ?: 0L
        val now = time.nowMillis()
        if (!force && now - last < source.ttlMillis) return
        try {
            repository.save(source.fetchCatalog())
            syncMeta.upsert(SyncMetaEntity(key, now))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logger.error(TAG, "Refreshing ${source.id.key} failed; keeping cached catalog", t)
        }
    }

    companion object {
        private const val TAG = "CatalogSyncer"
        fun metaKey(source: CatalogSource) = "catalog:${source.id.key}"
    }
}
