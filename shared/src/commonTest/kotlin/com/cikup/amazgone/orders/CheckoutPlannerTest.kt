package com.cikup.amazgone.orders

import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.testing.product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckoutPlannerTest {
    private val iphone = CartLine(product("1").copy(brand = "Apple"), 1)
    private val airpods = CartLine(product("2").copy(brand = "Apple"), 2)
    private val galaxy = CartLine(product("3").copy(brand = "Samsung"), 1)
    private val game = CartLine(
        product("g", category = "video-games").copy(source = CatalogSourceId.CHEAP_SHARK, brand = null, details = ProductDetails(sellerId = "1")),
        1,
    )

    @Test
    fun groupsLinesIntoOneShipmentPerStoreInCartOrder() {
        val shipments = CheckoutPlanner.shipments(listOf(iphone, galaxy, airpods))
        assertEquals(listOf("official-apple", "official-samsung"), shipments.map { it.store.id })
        assertEquals(listOf(iphone, airpods), shipments.first().lines)
    }

    @Test
    fun expressCostsCoinsButDigitalOnlyOrdersShipFree() {
        val physical = CheckoutPlanner.shipments(listOf(iphone, game))
        assertEquals(0, CheckoutPlanner.deliveryFee(physical, DeliveryOption.STANDARD))
        assertEquals(DeliveryOption.EXPRESS.feeCoins, CheckoutPlanner.deliveryFee(physical, DeliveryOption.EXPRESS))
        assertTrue(DeliveryOption.EXPRESS.feeCoins > 0)

        val digital = CheckoutPlanner.shipments(listOf(game))
        assertTrue(digital.single().isDigital)
        assertEquals(0, CheckoutPlanner.deliveryFee(digital, DeliveryOption.EXPRESS))
        assertTrue(!CheckoutPlanner.needsDelivery(digital))
    }

    @Test
    fun arrivalWindowFollowsTheOption() {
        val day = 24 * 60 * 60 * 1_000L
        val now = 1_000L
        assertEquals(now + 3 * day..now + 5 * day, CheckoutPlanner.arrival(now, DeliveryOption.STANDARD))
        assertEquals(now + day..now + day, CheckoutPlanner.arrival(now, DeliveryOption.EXPRESS))
    }
}
