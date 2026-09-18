package com.cikup.amazgone.cart

import com.cikup.amazgone.cart.data.repository.CartRepositoryImpl
import com.cikup.amazgone.cart.data.repository.OUTBOX_CART_SET
import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.core.database.RoomTransactionRunner
import com.cikup.amazgone.core.database.inMemoryDatabase
import com.cikup.amazgone.core.sync.data.RoomOutboxStore
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.SequentialIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartRepositoryTest {
    private val db = inMemoryDatabase()
    private val clock = FakeClock()
    private val outbox = RoomOutboxStore(db.outboxDao(), clock)
    private val repo = CartRepositoryImpl(db.cartDao(), outbox, RoomTransactionRunner(db), clock, SequentialIds())

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun everyChangeIsQueuedForSync() = runTest {
        repo.setQuantity("p", 2)
        repo.setQuantity("p", 0)

        val queued = outbox.head(10)
        assertEquals(listOf(OUTBOX_CART_SET, OUTBOX_CART_SET), queued.map { it.type })
        assertTrue(queued.last().payload.contains("\"quantity\":0"))
    }

    @Test
    fun removedItemsAreHiddenButKeptAsTombstones() = runTest {
        repo.setQuantity("p", 1)
        repo.setQuantity("p", 0)

        assertEquals(emptyList(), repo.observeEntries().first())
        assertEquals(listOf(CartEntry("p", 0, clock.now)), repo.snapshot())
    }

    @Test
    fun clearTombstonesEverythingInOneTransaction() = runTest {
        repo.setQuantity("a", 1)
        repo.setQuantity("b", 2)
        repo.clear()

        assertTrue(repo.observeEntries().first().isEmpty())
        assertEquals(4, outbox.head(10).size)
    }

    @Test
    fun remoteMergeIsLastWriteWins() = runTest {
        clock.now = 100
        repo.setQuantity("p", 1)

        repo.mergeRemote("p", 5, updatedAt = 50)
        assertEquals(1, repo.quantityOf("p"))

        repo.mergeRemote("p", 5, updatedAt = 200)
        assertEquals(5, repo.quantityOf("p"))
    }

    @Test
    fun readdingARemovedItemMovesItToTheEnd() = runTest {
        clock.now = 1; repo.setQuantity("a", 1)
        clock.now = 2; repo.setQuantity("b", 1)
        clock.now = 3; repo.setQuantity("a", 0)
        clock.now = 4; repo.setQuantity("a", 1)

        assertEquals(listOf("b", "a"), repo.observeEntries().first().map { it.productId })
    }
}
