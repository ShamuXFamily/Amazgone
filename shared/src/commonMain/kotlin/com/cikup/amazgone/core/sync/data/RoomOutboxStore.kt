package com.cikup.amazgone.core.sync.data

import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxStore
import kotlinx.coroutines.flow.Flow

class RoomOutboxStore(
    private val dao: OutboxDao,
    private val time: TimeProvider,
) : OutboxStore, UserScopedStore {

    override suspend fun enqueue(id: String, type: String, payload: String) =
        dao.insert(OutboxEntity(id = id, type = type, payload = payload, createdAt = time.nowMillis()))

    override suspend fun head(limit: Int): List<OutboxEntry> = dao.head(limit).map { it.toDomain() }

    override suspend fun remove(seq: Long) = dao.delete(seq)

    override suspend fun scheduleRetry(seq: Long, attempts: Int, nextAttemptAt: Long, error: String) =
        dao.scheduleRetry(seq, attempts, nextAttemptAt, error)

    override suspend fun park(seq: Long, error: String) = dao.park(seq, error)

    override suspend fun unparkAll() = dao.unparkAll()

    override fun observePendingCount(): Flow<Int> = dao.observeCount()

    /** Pending mutations belong to the signed-out user and must never reach the next account. */
    override suspend fun clearUserData() = dao.deleteAll()
}

private fun OutboxEntity.toDomain() = OutboxEntry(
    seq = seq,
    id = id,
    type = type,
    payload = payload,
    createdAt = createdAt,
    attempts = attempts,
    nextAttemptAt = nextAttemptAt,
    lastError = lastError,
    parked = parked,
)
