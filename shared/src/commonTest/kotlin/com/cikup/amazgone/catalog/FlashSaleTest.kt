package com.cikup.amazgone.catalog

import app.cash.turbine.test
import com.cikup.amazgone.catalog.domain.model.FlashSaleClock
import com.cikup.amazgone.catalog.domain.usecase.ObserveFlashSaleUseCase
import com.cikup.amazgone.testing.FakeCatalogRepository
import com.cikup.amazgone.testing.product
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlashSaleTest {
    private val hour = FlashSaleClock.WINDOW_MILLIS

    @Test
    fun windowsAlignToTheHour() {
        assertEquals(5 * hour, FlashSaleClock.windowStart(5 * hour + 123))
        val sale = FlashSaleClock.sale(emptyList(), 5 * hour + 10)
        assertEquals(4 * hour, sale.previousStart)
        assertEquals(6 * hour, sale.nextStart)
        assertEquals(hour - 10, sale.remainingMillis(5 * hour + 10))
    }

    @Test
    fun soldIsStableForAMomentAndGrowsThroughTheWindow() {
        val early = FlashSaleClock.soldFraction("dummyjson:1", 5 * hour + 1)
        assertEquals(early, FlashSaleClock.soldFraction("dummyjson:1", 5 * hour + 1))
        val late = FlashSaleClock.soldFraction("dummyjson:1", 6 * hour - 1)
        assertTrue(late > early)
        assertTrue(late <= 0.97f && early >= 0.15f)
    }

    @Test
    fun useCaseUsesDiscountedProducts() = runTest {
        val repo = FakeCatalogRepository(listOf(product("a", price = 5.0, original = 10.0), product("b", price = 5.0)))
        ObserveFlashSaleUseCase(repo)(flowOf(3 * hour)).test {
            assertEquals(listOf("dummyjson:a"), awaitItem().deals.map { it.product.id })
            cancelAndIgnoreRemainingEvents() // catalog flow is hot
        }
    }
}
