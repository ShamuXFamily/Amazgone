package com.cikup.amazgone.core.sync.domain.usecase

import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.core.sync.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Combines connectivity, queue size and last failure into a user-visible status. */
class ObserveSyncStatusUseCase(
    private val connectivity: ConnectivityObserver,
    private val store: OutboxStore,
    private val engine: SyncEngine,
) {
    operator fun invoke(): Flow<SyncStatus> = combine(
        connectivity.isOnline,
        store.observePendingCount(),
        engine.lastError,
    ) { online, pending, error -> SyncStatus.from(online, pending, error) }
        .distinctUntilChanged()
}
