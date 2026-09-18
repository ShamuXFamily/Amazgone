package com.cikup.amazgone.core.sync.domain

import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Drains the outbox to the backend (strict FIFO, exponential backoff, idempotent ids),
 * then refreshes local data through [RemotePuller]s. Pushes always run before pulls so
 * pulled server state already contains this device's changes.
 */
class SyncEngine(
    private val store: OutboxStore,
    handlers: List<OutboxHandler>,
    private val pullers: List<RemotePuller>,
    private val gate: SyncGate,
    private val connectivity: ConnectivityObserver,
    private val time: TimeProvider,
    private val logger: AppLogger,
) {
    private val handlersByType = handlers.associateBy { it.type }
    private val mutex = Mutex()

    private val mutableLastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = mutableLastError.asStateFlow()

    private val mutableSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = mutableSyncing.asStateFlow()

    suspend fun sync(forcePull: Boolean = false) = mutex.withLock {
        if (!connectivity.isOnline.value) return@withLock
        mutableSyncing.value = true
        if (forcePull) store.unparkAll() // an explicit user refresh retries now: un-park + skip backoff
        try {
            val canPush = gate.canPush()
            if (canPush) drain()
            pullAll(forcePull, canPush)
        } finally {
            mutableSyncing.value = false
        }
    }

    private suspend fun drain() {
        while (true) {
            val batch = store.head(BATCH_SIZE)
            if (batch.isEmpty()) {
                mutableLastError.value = null
                return
            }
            for (entry in batch) {
                if (!process(entry)) return
            }
        }
    }

    /** @return false when the queue must stop (entry is waiting for its backoff). */
    private suspend fun process(entry: OutboxEntry): Boolean {
        if (entry.nextAttemptAt > time.nowMillis()) return false
        val handler = handlersByType[entry.type]
        if (handler == null) {
            logger.error(TAG, "No handler for outbox type '${entry.type}', dropping ${entry.id}")
            store.remove(entry.seq)
            return true
        }
        return when (val result = pushSafely(handler, entry)) {
            PushResult.Success -> {
                store.remove(entry.seq)
                true
            }
            is PushResult.Rejected -> {
                reject(handler, entry, result.reason)
                true
            }
            is PushResult.Retry -> scheduleRetry(handler, entry, result.reason)
        }
    }

    private suspend fun pushSafely(handler: OutboxHandler, entry: OutboxEntry): PushResult = try {
        handler.push(entry)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        logger.error(TAG, "Push failed for ${entry.type}/${entry.id}", t)
        PushResult.Retry(t.message ?: t::class.simpleName.orEmpty())
    }

    /**
     * Transient failures never compensate: after [Backoff.MAX_ATTEMPTS] the entry is parked (kept, still
     * counted as pending, skipped by the queue) so it can't block other changes. Only an explicit
     * [PushResult.Rejected] from the server reverses local state.
     */
    private suspend fun scheduleRetry(handler: OutboxHandler, entry: OutboxEntry, reason: String): Boolean {
        val attempts = entry.attempts + 1
        mutableLastError.value = reason
        if (attempts >= Backoff.MAX_ATTEMPTS) {
            logger.error(TAG, "Parking ${handler.type}/${entry.id} after $attempts attempts: $reason")
            store.park(entry.seq, reason)
            return true
        }
        store.scheduleRetry(entry.seq, attempts, time.nowMillis() + Backoff.delayMillis(attempts), reason)
        return false
    }

    private suspend fun reject(handler: OutboxHandler, entry: OutboxEntry, reason: String) {
        logger.error(TAG, "Rejected ${entry.type}/${entry.id}: $reason")
        handler.onRejected(entry, reason)
        store.remove(entry.seq)
    }

    private suspend fun pullAll(force: Boolean, signedIn: Boolean) {
        pullers.filter { signedIn || !it.requiresAuth }.forEach { puller ->
            try {
                puller.pull(force)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                logger.error(TAG, "Pull '${puller.name}' failed", t)
            }
        }
    }

    private companion object {
        const val TAG = "SyncEngine"
        const val BATCH_SIZE = 20
    }
}
