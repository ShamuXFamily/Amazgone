package com.cikup.amazgone.testing

import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeClock(var now: Long = 1_000_000L) : TimeProvider {
    override fun nowMillis(): Long = now
    fun advance(millis: Long) { now += millis }
}

class SequentialIds(private val prefix: String = "id") : IdGenerator {
    private var next = 0
    override fun newId(): String = "$prefix-${++next}"
}

class FakeConnectivity(online: Boolean = true) : ConnectivityObserver {
    override val isOnline = MutableStateFlow(online)
}

class FakeOutboxStore(private val clock: FakeClock = FakeClock()) : OutboxStore {
    private val entries = MutableStateFlow<List<OutboxEntry>>(emptyList())
    private var seq = 0L
    val all: List<OutboxEntry> get() = entries.value

    override suspend fun enqueue(id: String, type: String, payload: String) {
        entries.value = entries.value + OutboxEntry(++seq, id, type, payload, clock.now, 0, 0, null)
    }

    override suspend fun head(limit: Int) = entries.value.filterNot { it.parked }.sortedBy { it.seq }.take(limit)

    override suspend fun park(seq: Long, error: String) {
        entries.value = entries.value.map { if (it.seq == seq) it.copy(parked = true, lastError = error) else it }
    }

    override suspend fun unparkAll() {
        entries.value = entries.value.map { if (it.parked) it.copy(parked = false, attempts = 0, nextAttemptAt = 0) else it.copy(nextAttemptAt = 0) }
    }

    override suspend fun remove(seq: Long) {
        entries.value = entries.value.filterNot { it.seq == seq }
    }

    override suspend fun scheduleRetry(seq: Long, attempts: Int, nextAttemptAt: Long, error: String) {
        entries.value = entries.value.map {
            if (it.seq == seq) it.copy(attempts = attempts, nextAttemptAt = nextAttemptAt, lastError = error) else it
        }
    }

    override fun observePendingCount(): Flow<Int> = entries.map { it.size }
}

val StateFlow<Boolean>.asFake get() = this as MutableStateFlow<Boolean>
