package com.cikup.amazgone.core.sync

import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.SyncCoordinator
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.games.presentation.formatCountdown
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.FakeConnectivity
import com.cikup.amazgone.testing.FakeOutboxStore
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncCoordinatorTest {
    private val clock = FakeClock()
    private val store = FakeOutboxStore(clock)
    private val connectivity = FakeConnectivity(online = false)
    private val pushed = mutableListOf<String>()
    private val handler = object : OutboxHandler {
        override val type = "t"
        override suspend fun push(entry: OutboxEntry): PushResult { pushed += entry.id; return PushResult.Success }
        override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
    }
    private val engine = SyncEngine(store, listOf(handler), emptyList(), { true }, connectivity, clock, PrintLogger)

    @Test
    fun queuedWorkIsPushedWhenConnectivityReturns() = runTest {
        val job = backgroundScope
        SyncCoordinator(engine, store, connectivity).start(job)
        store.enqueue("a", "t", "{}")
        advanceTimeBy(2_000)
        assertEquals(emptyList(), pushed, "offline: nothing is sent")

        connectivity.isOnline.value = true
        runCurrent()
        advanceTimeBy(2_000)
        assertEquals(listOf("a"), pushed)
    }

    @Test
    fun newMutationsAreDebouncedThenPushed() = runTest {
        connectivity.isOnline.value = true
        SyncCoordinator(engine, store, connectivity).start(backgroundScope)
        runCurrent()
        store.enqueue("b", "t", "{}")
        advanceTimeBy(100)
        assertEquals(emptyList(), pushed)
        advanceTimeBy(1_000)
        assertEquals(listOf("b"), pushed)
    }

    @Test
    fun manualRequestSyncs() = runTest {
        connectivity.isOnline.value = true
        val coordinator = SyncCoordinator(engine, store, connectivity)
        coordinator.start(backgroundScope)
        runCurrent()
        store.enqueue("c", "t", "{}")
        coordinator.requestSync(force = true)
        runCurrent()
        assertEquals(listOf("c"), pushed)
    }
}

class SmallFormattingTest {
    @Test
    fun countdownIsZeroPaddedAndRoundsUp() {
        assertEquals("00:00:00", formatCountdown(0))
        assertEquals("00:00:01", formatCountdown(1))
        assertEquals("01:02:03", formatCountdown(3_723_000))
        assertEquals("24:00:00", formatCountdown(24 * 3_600_000L))
    }

    @Test
    fun documentPathsAreStable() {
        assertEquals("users/u/orders/o", UserDocuments.order("u", "o"))
        assertEquals("users/u/cart/dummyjson:1", UserDocuments.cartItem("u", "dummyjson:1"))
        assertEquals("a_b", UserDocuments.encodeId("a/b"))
        assertEquals(2, UserDocuments.createProfileWrites("u", "bob").size, "profile (with createdAt transform) + leaderboard row")
    }
}
