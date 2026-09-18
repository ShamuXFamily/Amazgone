package com.cikup.amazgone.core.database

import app.cash.turbine.test
import com.cikup.amazgone.core.sync.data.OutboxEntity
import com.cikup.amazgone.core.sync.data.RoomOutboxStore
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OutboxDaoTest {
    private val db = inMemoryDatabase()
    private val store = RoomOutboxStore(db.outboxDao()) { 42L }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun headReturnsEntriesInInsertionOrder() = runTest {
        store.enqueue("b", "t", "{}")
        store.enqueue("a", "t", "{}")

        assertEquals(listOf("b", "a"), store.head(10).map { it.id })
        assertEquals(42L, store.head(1).single().createdAt)
    }

    @Test
    fun enqueueIsIdempotentPerId() = runTest {
        store.enqueue("same", "t", "{}")
        store.enqueue("same", "t", "{\"dup\":true}")

        assertEquals(1, store.head(10).size)
    }

    @Test
    fun retryAndRemoveUpdateTheQueue() = runTest {
        store.enqueue("a", "t", "{}")
        val seq = store.head(1).single().seq

        store.scheduleRetry(seq, attempts = 2, nextAttemptAt = 99, error = "offline")
        val retried = store.head(1).single()
        assertEquals(2, retried.attempts)
        assertEquals("offline", retried.lastError)

        store.remove(seq)
        assertEquals(emptyList(), store.head(10))
    }

    @Test
    fun pendingCountIsObservable() = runTest {
        store.observePendingCount().test {
            assertEquals(0, awaitItem())
            db.outboxDao().insert(OutboxEntity(id = "x", type = "t", payload = "{}", createdAt = 0))
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transactionRollsBackAllWritesOnFailure() = runTest {
        val runner = RoomTransactionRunner(db)
        runCatching {
            runner.inTransaction {
                store.enqueue("a", "t", "{}")
                error("boom")
            }
        }
        assertEquals(0, store.head(10).size)
    }
}
