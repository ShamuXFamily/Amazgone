package com.cikup.amazgone.core.sync.domain

import kotlinx.coroutines.flow.Flow

/** A local mutation waiting to be pushed. [id] doubles as the remote idempotency key. */
data class OutboxEntry(
    val seq: Long,
    val id: String,
    val type: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int,
    val nextAttemptAt: Long,
    val lastError: String?,
    /** Gave up after [Backoff.MAX_ATTEMPTS]: skipped by the queue (never auto-reversed) until a manual retry. */
    val parked: Boolean = false,
)

sealed interface PushResult {
    data object Success : PushResult
    /** Transient failure (network, auth not ready): keep the entry and back off. */
    data class Retry(val reason: String) : PushResult
    /** Permanent failure (server refused): the handler must compensate local state. */
    data class Rejected(val reason: String) : PushResult
}

/** Pushes one outbox [type] to the backend and reconciles local state afterwards. */
interface OutboxHandler {
    val type: String
    suspend fun push(entry: OutboxEntry): PushResult
    /** Undo the optimistic local effect after a permanent failure (refund, restore cart…). */
    suspend fun onRejected(entry: OutboxEntry, reason: String)
}

/** Storage port for the outbox; implemented with Room, faked in tests. */
interface OutboxStore {
    suspend fun enqueue(id: String, type: String, payload: String)
    /** Oldest non-parked entries first (strict FIFO), regardless of backoff. */
    suspend fun head(limit: Int): List<OutboxEntry>
    suspend fun remove(seq: Long)
    suspend fun scheduleRetry(seq: Long, attempts: Int, nextAttemptAt: Long, error: String)
    /** Moves an entry out of the FIFO path without dropping it. */
    suspend fun park(seq: Long, error: String)
    /** User pressed "Sync now": parked entries rejoin the queue and backoff waits are skipped. */
    suspend fun unparkAll()
    fun observePendingCount(): Flow<Int>
}
