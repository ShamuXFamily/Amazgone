package com.cikup.amazgone.cart

import app.cash.turbine.test
import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartCountUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.cart.domain.usecase.UpdateCartQuantityUseCase
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.testing.FakeCartRepository
import com.cikup.amazgone.testing.FakeCatalogRepository
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.product
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CartDomainTest {
    private val phone = product("1", price = 10.0, original = 20.0) // 100 coins, was 200
    private val case = product("2", price = 2.5) // 25 coins

    @Test
    fun summaryAddsLinesAndSavings() {
        val summary = CartCalculator.summarize(listOf(CartLine(phone, 2), CartLine(case, 1)))
        assertEquals(3, summary.itemCount)
        assertEquals(225, summary.subtotalCoins)
        assertEquals(200, summary.savingsCoins)
        assertEquals(225, summary.totalCoins)
    }

    @Test
    fun percentCouponRoundsDown() {
        assertEquals(22, CartCalculator.applyCoupon(225, Coupon("TEN", CouponKind.PERCENT, 10)))
    }

    @Test
    fun couponsNeverMakeAnOrderFree() {
        assertEquals(99, CartCalculator.applyCoupon(100, Coupon("BIG", CouponKind.FLAT_COINS, 5_000)))
        assertEquals(99, CartCalculator.applyCoupon(100, Coupon("ALL", CouponKind.PERCENT, 150)))
        assertEquals(0, CartCalculator.applyCoupon(0, Coupon("X", CouponKind.FLAT_COINS, 10)))
    }

    @Test
    fun couponRespectsMinimumAndExpiry() {
        val lines = listOf(CartLine(case, 1))
        val needsMore = Coupon("MIN", CouponKind.FLAT_COINS, 10, minSubtotalCoins = 500)
        val expired = Coupon("OLD", CouponKind.FLAT_COINS, 10, expiresAt = 50)
        assertNull(CartCalculator.summarize(lines, needsMore, now = 0).appliedCoupon)
        assertNull(CartCalculator.summarize(lines, expired, now = 100).appliedCoupon)
        val ok = CartCalculator.summarize(lines, expired, now = 10)
        assertEquals(15, ok.totalCoins)
    }

    @Test
    fun addToCartIncrementsUpToTheLimit() = runTest {
        val cart = FakeCartRepository()
        val add = AddToCartUseCase(cart)
        repeat(CartCalculator.MAX_QUANTITY_PER_ITEM) { add("p") }

        assertIs<DomainResult.Failure>(add("p"))
        assertEquals(CartCalculator.MAX_QUANTITY_PER_ITEM, cart.quantityOf("p"))
    }

    @Test
    fun updatingToZeroRemovesAndQuantitiesAreClamped() = runTest {
        val cart = FakeCartRepository()
        val update = UpdateCartQuantityUseCase(cart)
        update("p", 99)
        assertEquals(CartCalculator.MAX_QUANTITY_PER_ITEM, cart.quantityOf("p"))
        update("p", 0)
        assertEquals(0, cart.quantityOf("p"))
    }

    @Test
    fun observeCartJoinsProductsAndSkipsUnknownOnes() = runTest {
        val cart = FakeCartRepository()
        cart.setQuantity(phone.id, 2)
        cart.setQuantity("dummyjson:ghost", 1)
        val useCase = ObserveCartUseCase(cart, FakeCatalogRepository(listOf(phone)), FakeClock())

        useCase(flowOf(null)).test {
            val summary = awaitItem()
            assertEquals(1, summary.lines.size)
            assertEquals(200, summary.totalCoins)
            cart.setQuantity(phone.id, 0)
            assertTrue(awaitItem().isEmpty)
        }
    }

    @Test
    fun cartCountSumsQuantities() = runTest {
        val cart = FakeCartRepository()
        ObserveCartCountUseCase(cart)().test {
            assertEquals(0, awaitItem())
            cart.setQuantity("a", 2)
            assertEquals(2, awaitItem())
            cart.setQuantity("b", 3)
            assertEquals(5, awaitItem())
        }
    }
}
