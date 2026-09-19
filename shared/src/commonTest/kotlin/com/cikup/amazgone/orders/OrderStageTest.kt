package com.cikup.amazgone.orders

import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.model.canConfirmReceived
import com.cikup.amazgone.orders.domain.model.isItemDelivered
import com.cikup.amazgone.orders.domain.model.stageAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderStageTest {
    private val hour = 60 * 60 * 1_000L
    private val day = 24 * hour
    private val parcel = OrderItem("p", "Charger", "", 1, 200, "Apple Official")
    private val code = OrderItem("g", "Game", "", 1, 1, "Steam", digital = true)
    private fun order(status: OrderStatus, vararg items: OrderItem, deliveredAt: Long? = null) = Order(
        id = "o", items = items.toList(), subtotalCoins = 0, discountCoins = 0, totalCoins = 0, couponCode = null,
        address = ShippingAddress("a", "b", "c", "12345", "d"), delivery = DeliveryOption.STANDARD, deliveryFeeCoins = 0,
        status = status, xpEarned = 0, createdAt = 0, rejectionReason = null, deliveredAt = deliveredAt,
    )

    @Test
    fun parcelsMoveThroughTheTrackerWithTime() {
        val o = order(OrderStatus.CONFIRMED, parcel)
        assertEquals(OrderStage.CONFIRMED, o.stageAt(hour))
        assertEquals(OrderStage.SHIPPED, o.stageAt(13 * hour))
        assertEquals(OrderStage.DELIVERED, o.stageAt(3 * day)) // standard arrives 3–5 days
    }

    @Test
    fun unsyncedAndRejectedOrdersNeverAdvance() {
        assertEquals(OrderStage.PLACED, order(OrderStatus.PENDING_SYNC, parcel).stageAt(10 * day))
        assertEquals(OrderStage.REJECTED, order(OrderStatus.REJECTED, parcel).stageAt(10 * day))
        assertEquals(OrderStage.PLACED, order(OrderStatus.PENDING_SYNC, code).stageAt(0))
    }

    @Test
    fun digitalOnlyOrdersAreDeliveredOnceConfirmed() {
        assertEquals(OrderStage.DELIVERED, order(OrderStatus.CONFIRMED, code).stageAt(0))
    }

    @Test
    fun customerCanConfirmReceiptOfAConfirmedParcel() {
        val o = order(OrderStatus.CONFIRMED, parcel, code)
        assertTrue(o.canConfirmReceived(hour))
        assertEquals(OrderStage.DELIVERED, o.copy(deliveredAt = hour).stageAt(hour))
        assertFalse(o.copy(deliveredAt = hour).canConfirmReceived(hour))
        assertFalse(order(OrderStatus.PENDING_SYNC, parcel).canConfirmReceived(hour))
    }

    @Test
    fun digitalItemsInAMixedOrderAreReviewableOnceConfirmed() {
        val o = order(OrderStatus.CONFIRMED, parcel, code)
        assertTrue(o.isItemDelivered(code, hour))
        assertFalse(o.isItemDelivered(parcel, hour))
        assertFalse(order(OrderStatus.PENDING_SYNC, code).isItemDelivered(code, hour))
    }
}
