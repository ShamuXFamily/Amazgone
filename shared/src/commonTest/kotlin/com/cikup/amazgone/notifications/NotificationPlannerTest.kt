package com.cikup.amazgone.notifications

import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.notifications.domain.model.NotificationInputs
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.NotificationPlanner
import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.model.parcels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationPlannerTest {
    private val hour = 60 * 60 * 1_000L
    private val since = 1_000L
    private val charger = OrderItem("p", "iPhone Charger", "", 1, 200, "Apple Official", storeId = "official-apple")
    private fun order(status: OrderStatus, createdAt: Long = since + 10) = Order(
        id = "o1", items = listOf(charger), subtotalCoins = 200, discountCoins = 0, totalCoins = 200, couponCode = null,
        address = ShippingAddress("Bob", "1 Main", "Jakarta", "10110", "Indonesia", lat = -6.2, lon = 106.8),
        delivery = DeliveryOption.STANDARD, deliveryFeeCoins = 0, status = status, xpEarned = 0, createdAt = createdAt,
        rejectionReason = null, courier = Courier.PIGEON,
    )
    private fun inputs(vararg orders: Order, now: Long = since + 20) = NotificationInputs(since = since, now = now, orders = orders.toList())

    @Test
    fun aConfirmedCourierOrderPlansItsWholeJourney() {
        val o = order(OrderStatus.CONFIRMED)
        val plan = NotificationPlanner.plan(inputs(o))
        assertEquals(
            listOf(NotificationKind.ORDER_CONFIRMED, NotificationKind.COURIER_DEPARTED, NotificationKind.PARCEL_DELIVERED, NotificationKind.REVIEW_PROMPT),
            plan.sortedBy { it.deliverAt }.map { it.kind },
        )
        val arrival = o.parcels().single().arrivalAt
        assertEquals(arrival, plan.first { it.kind == NotificationKind.PARCEL_DELIVERED }.deliverAt)
        assertEquals(arrival + hour, plan.first { it.kind == NotificationKind.REVIEW_PROMPT }.deliverAt)
        assertTrue(plan.all { it.link == "order/o1" })
        assertEquals(plan.map { it.id }.toSet().size, plan.size) // stable, unique ids → never notified twice
    }

    @Test
    fun pendingOrdersWaitAndRefusedOrdersOnlySayRefunded() {
        assertTrue(NotificationPlanner.plan(inputs(order(OrderStatus.PENDING_SYNC))).isEmpty())
        assertEquals(listOf(NotificationKind.ORDER_REJECTED), NotificationPlanner.plan(inputs(order(OrderStatus.REJECTED))).map { it.kind })
    }

    @Test
    fun nothingFromBeforeTheFeatureWasSwitchedOn() {
        val old = order(OrderStatus.CONFIRMED, createdAt = 0).copy(courier = null) // delivered long ago
        assertTrue(NotificationPlanner.plan(inputs(old)).isEmpty())
    }

    @Test
    fun gameCooldownsAndACappedFlashReminder() {
        val plan = NotificationPlanner.plan(
            NotificationInputs(since = since, now = since, lastPlayed = mapOf(GameKind.SPIN to since + 5), flashWindowEnd = since + hour, lastFlashReminderAt = null),
        )
        assertEquals(since + 5 + GameKind.SPIN.cooldownMillis, plan.single { it.kind == NotificationKind.SPIN_READY }.deliverAt)
        assertEquals(since + hour - NotificationPlanner.FLASH_LEAD_MILLIS, plan.single { it.kind == NotificationKind.FLASH_SALE }.deliverAt)

        val capped = NotificationPlanner.plan(
            NotificationInputs(since = since, now = since, flashWindowEnd = since + hour, lastFlashReminderAt = since),
        )
        assertTrue(capped.none { it.kind == NotificationKind.FLASH_SALE }) // at most one flash reminder a day
    }

    @Test
    fun unlocksBecomeInboxEntries() {
        val plan = NotificationPlanner.plan(
            NotificationInputs(
                since = since, now = since + 100,
                achievements = mapOf("FIRST_ORDER" to since + 50, "OLD_ONE" to since - 50),
                couriers = mapOf(Courier.CARGO_JET to since + 60, Courier.PIGEON to since + 1),
            ),
        )
        assertEquals(setOf("achievement-FIRST_ORDER", "courier-CARGO_JET"), plan.map { it.id }.toSet())
    }
}
