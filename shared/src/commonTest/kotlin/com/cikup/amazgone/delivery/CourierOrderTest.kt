package com.cikup.amazgone.delivery

import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.model.canConfirmReceived
import com.cikup.amazgone.orders.domain.model.isItemDelivered
import com.cikup.amazgone.orders.domain.model.parcels
import com.cikup.amazgone.orders.domain.model.stageAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourierOrderTest {
    private val hour = 60 * 60 * 1_000L
    private val day = 24 * hour
    private val jakarta = ShippingAddress("Bob", "1 Main", "Jakarta", "10110", "Indonesia", lat = -6.2, lon = 106.8)
    private val iphone = OrderItem("a", "iPhone", "", 1, 100, "Apple Official", storeId = "official-apple")
    private val rice = OrderItem("b", "Rice", "", 1, 10, "Amazgone Fresh", storeId = "amazgone-fresh")
    private val game = OrderItem("g", "Game", "", 1, 1, "Steam", digital = true, storeId = "digital-1")

    private fun order(courier: Courier, vararg items: OrderItem) = Order(
        id = "o", items = items.toList(), subtotalCoins = 0, discountCoins = 0, totalCoins = 0, couponCode = null,
        address = jakarta, delivery = DeliveryOption.STANDARD, deliveryFeeCoins = 0, status = OrderStatus.CONFIRMED,
        xpEarned = 0, createdAt = 0, rejectionReason = null, courier = courier,
    )

    @Test
    fun eachStoreParcelHasItsOwnJourney() {
        val parcels = order(Courier.PIGEON, iphone, rice, game).parcels()
        assertEquals(listOf("Cupertino", "Jakarta"), parcels.map { it.origin.city }) // the game is a code, no parcel
        val (fromApple, local) = parcels
        assertTrue(fromApple.arrivalAt > 20 * day, "a pigeon from Cupertino takes weeks")
        assertTrue(local.arrivalAt < day, "a pigeon across Jakarta is quick")
    }

    @Test
    fun theOrderIsInTransitUntilTheLastParcelArrives() {
        val o = order(Courier.CARGO_JET, iphone, rice)
        assertEquals(OrderStage.CONFIRMED, o.stageAt(CourierPlan.HANDLING_MILLIS / 2)) // still packing
        assertEquals(OrderStage.SHIPPED, o.stageAt(CourierPlan.HANDLING_MILLIS + hour))
        val last = o.parcels().maxOf { it.arrivalAt }
        assertEquals(OrderStage.DELIVERED, o.stageAt(last))
        assertTrue(o.isItemDelivered(rice, o.parcels().first { it.origin.city == "Jakarta" }.arrivalAt))
        assertFalse(o.isItemDelivered(iphone, o.parcels().first { it.origin.city == "Jakarta" }.arrivalAt))
    }

    @Test
    fun courierOrdersCannotBeMarkedReceivedEarly() {
        assertFalse(order(Courier.PIGEON, iphone).canConfirmReceived(day))
    }
}
