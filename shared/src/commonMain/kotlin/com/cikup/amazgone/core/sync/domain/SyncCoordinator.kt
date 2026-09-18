package com.cikup.amazgone.core.sync.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Decides *when* to sync while the app is alive: on start, whenever connectivity returns,
 * shortly after new local mutations, periodically, and on explicit user refresh.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val store: OutboxStore,
    private val connectivity: ConnectivityObserver,
) {
    private val manualRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    fun requestSync(force: Boolean = false) {
        manualRequests.tryEmit(force)
    }

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        val cameOnline: Flow<Boolean> = connectivity.isOnline
            .filter { it }
            .map { false }
        val newWork: Flow<Boolean> = combine(connectivity.isOnline, store.observePendingCount()) { online, pending ->
            online && pending > 0
        }
            .filter { it }
            .debounce(MUTATION_DEBOUNCE_MS)
            .map { false }

        merge(cameOnline, newWork, manualRequests)
            .onEach { force -> engine.sync(forcePull = force) }
            .launchIn(scope)

        scope.launch {
            while (isActive) {
                delay(PERIODIC_SYNC_MS)
                engine.sync()
            }
        }
    }

    private companion object {
        const val MUTATION_DEBOUNCE_MS = 750L
        const val PERIODIC_SYNC_MS = 5 * 60 * 1_000L
    }
}
