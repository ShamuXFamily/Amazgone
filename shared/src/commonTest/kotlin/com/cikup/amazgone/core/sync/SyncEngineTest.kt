package com.cikup.amazgone.core.sync

import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.sync.domain.Backoff
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.FakeConnectivity
import com.cikup.amazgone.testing.FakeOutboxStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class ScriptedHandler(
    override val type: String,
    private val script: MutableList<PushResult> = mutableListOf(),
) : OutboxHandler {
    val pushedIds = mutableListOf<String>()
    val rejected = mutableListOf<Pair<String, String>>()

    override suspend fun push(entry: OutboxEntry): PushResult {
        pushedIds += entry.id
        return if (script.isEmpty()) PushResult.Success else script.removeAt(0)
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        rejected += entry.id to reason
    }
}

private class CountingPuller(override val requiresAuth: Boolean) : RemotePuller {
    override val name = "counting"
    var pulls = 0
    override suspend fun pull(force: Boolean) { pulls++ }
}

class SyncEngineTest {
    private val clock = FakeClock()
    private val store = FakeOutboxStore(clock)
    private val connectivity = FakeConnectivity(online = true)
    private var signedIn = true

    private fun engine(vararg handlers: OutboxHandler, pullers: List<RemotePuller> = emptyList()) = SyncEngine(
        store = store,
        handlers = handlers.toList(),
        pullers = pullers,
        gate = { signedIn },
        connectivity = connectivity,
        time = clock,
        logger = PrintLogger,
    )

    @Test
    fun pushesEntriesInFifoOrderAndRemovesThem() = runTest {
        val handler = ScriptedHandler("order")
        store.enqueue("a", "order", "{}")
        store.enqueue("b", "order", "{}")

        engine(handler).sync()

        assertEquals(listOf("a", "b"), handler.pushedIds)
        assertTrue(store.all.isEmpty())
    }

    @Test
    fun retryStopsTheQueueAndSchedulesBackoff() = runTest {
        val handler = ScriptedHandler("order", mutableListOf(PushResult.Retry("timeout")))
        store.enqueue("a", "order", "{}")
        store.enqueue("b", "order", "{}")

        engine(handler).sync()

        assertEquals(listOf("a"), handler.pushedIds, "b must wait behind a (strict FIFO)")
        val head = store.all.first()
        assertEquals(1, head.attempts)
        assertEquals(clock.now + Backoff.delayMillis(1), head.nextAttemptAt)
    }

    @Test
    fun entryIsNotRetriedBeforeItsBackoffElapses() = runTest {
        val handler = ScriptedHandler("order", mutableListOf(PushResult.Retry("timeout")))
        store.enqueue("a", "order", "{}")
        val engine = engine(handler)

        engine.sync()
        engine.sync()
        assertEquals(1, handler.pushedIds.size)

        clock.advance(Backoff.delayMillis(1))
        engine.sync()
        assertEquals(listOf("a", "a"), handler.pushedIds)
        assertTrue(store.all.isEmpty())
    }

    @Test
    fun retriedPushReusesTheSameIdempotencyKey() = runTest {
        val handler = ScriptedHandler("order", mutableListOf(PushResult.Retry("x"), PushResult.Retry("x")))
        store.enqueue("order-42", "order", "{}")
        val engine = engine(handler)

        repeat(3) { engine.sync(); clock.advance(Backoff.MAX_MILLIS) }

        assertEquals(listOf("order-42", "order-42", "order-42"), handler.pushedIds)
    }

    @Test
    fun rejectionCompensatesAndContinues() = runTest {
        val handler = ScriptedHandler("order", mutableListOf(PushResult.Rejected("insufficient coins")))
        store.enqueue("a", "order", "{}")
        store.enqueue("b", "order", "{}")

        engine(handler).sync()

        assertEquals(listOf("a" to "insufficient coins"), handler.rejected)
        assertEquals(listOf("a", "b"), handler.pushedIds)
        assertTrue(store.all.isEmpty())
    }

    @Test
    fun exhaustedEntriesAreParkedNotReversedAndStopBlockingTheQueue() = runTest {
        val failures = MutableList<PushResult>(Backoff.MAX_ATTEMPTS) { PushResult.Retry("down") }
        val handler = ScriptedHandler("order", failures)
        store.enqueue("a", "order", "{}")
        store.enqueue("b", "order", "{}")
        val engine = engine(handler)

        repeat(Backoff.MAX_ATTEMPTS) { engine.sync(); clock.advance(Backoff.MAX_MILLIS) }

        assertTrue(handler.rejected.isEmpty(), "a transient failure must never refund/reverse")
        assertEquals(listOf("a"), store.all.map { it.id }, "b went through; a is kept, parked")
        assertTrue(store.all.single().parked)

        engine.sync(forcePull = true) // user pressed "Sync now"
        assertTrue(store.all.isEmpty())
    }

    @Test
    fun thrownExceptionsAreTreatedAsRetry() = runTest {
        val handler = object : OutboxHandler {
            override val type = "boom"
            override suspend fun push(entry: OutboxEntry): PushResult = error("socket closed")
            override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
        }
        store.enqueue("a", "boom", "{}")

        engine(handler).sync()

        assertEquals(1, store.all.single().attempts)
        assertEquals("socket closed", store.all.single().lastError)
    }

    @Test
    fun doesNothingWhileOffline() = runTest {
        val handler = ScriptedHandler("order")
        val puller = CountingPuller(requiresAuth = false)
        connectivity.isOnline.value = false
        store.enqueue("a", "order", "{}")

        engine(handler, pullers = listOf(puller)).sync()

        assertTrue(handler.pushedIds.isEmpty())
        assertEquals(0, puller.pulls)
    }

    @Test
    fun guestsKeepTheirOutboxButStillPullPublicData() = runTest {
        signedIn = false
        val handler = ScriptedHandler("order")
        val publicPuller = CountingPuller(requiresAuth = false)
        val privatePuller = CountingPuller(requiresAuth = true)
        store.enqueue("a", "order", "{}")

        engine(handler, pullers = listOf(publicPuller, privatePuller)).sync()

        assertTrue(handler.pushedIds.isEmpty())
        assertEquals(1, store.all.size)
        assertEquals(1, publicPuller.pulls)
        assertEquals(0, privatePuller.pulls)
    }

    @Test
    fun unknownTypesAreDroppedInsteadOfBlockingTheQueue() = runTest {
        val handler = ScriptedHandler("order")
        store.enqueue("legacy", "removed-type", "{}")
        store.enqueue("a", "order", "{}")

        engine(handler).sync()

        assertEquals(listOf("a"), handler.pushedIds)
        assertTrue(store.all.isEmpty())
    }

    @Test
    fun pullsRunAfterPushesSoServerStateIncludesThem() = runTest {
        val order = mutableListOf<String>()
        val handler = object : OutboxHandler {
            override val type = "order"
            override suspend fun push(entry: OutboxEntry): PushResult { order += "push"; return PushResult.Success }
            override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
        }
        val puller = object : RemotePuller {
            override val name = "p"
            override val requiresAuth = true
            override suspend fun pull(force: Boolean) { order += "pull" }
        }
        store.enqueue("a", "order", "{}")

        engine(handler, pullers = listOf(puller)).sync()

        assertEquals(listOf("push", "pull"), order)
    }

    @Test
    fun failingPullerDoesNotStopOthers() = runTest {
        val broken = object : RemotePuller {
            override val name = "broken"
            override val requiresAuth = false
            override suspend fun pull(force: Boolean) = error("500")
        }
        val healthy = CountingPuller(requiresAuth = false)

        engine(pullers = listOf(broken, healthy)).sync()

        assertEquals(1, healthy.pulls)
    }
}
