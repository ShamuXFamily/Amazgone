package com.cikup.amazgone.notifications.domain.model

import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.parcels

enum class NotificationKind {
    ORDER_CONFIRMED, ORDER_REJECTED, COURIER_DEPARTED, PARCEL_DELIVERED, REVIEW_PROMPT,
    SPIN_READY, SCRATCH_READY, FLASH_SALE,
    LEVEL_UP, ACHIEVEMENT, COURIER_UNLOCKED,
}

/**
 * Something the app will (or did) tell the user. [id] is stable for the event it describes, so
 * planning again never duplicates it; [args] are filled into the localized title/body at render time.
 */
data class PlannedNotification(
    val id: String,
    val kind: NotificationKind,
    val deliverAt: Long,
    val link: String,
    val args: List<String> = emptyList(),
)

/** A notification in the inbox (rendered text, read state). */
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val link: String,
    val deliverAt: Long,
    val read: Boolean,
)

/** Deep links understood by the app shell (also carried by system notifications). */
object NotificationLinks {
    fun order(id: String) = "order/$id"
    const val SPIN = "spin"
    const val SCRATCH = "scratch"
    const val FLASH_SALE = "flash"
    const val GARAGE = "garage"
    const val ACHIEVEMENTS = "achievements"
}

/** Everything the planner looks at; the coordinator gathers it from the repositories. */
data class NotificationInputs(
    /** Events before this moment are history, not news (set when notifications were first switched on). */
    val since: Long,
    val now: Long,
    val orders: List<Order> = emptyList(),
    val lastPlayed: Map<GameKind, Long> = emptyMap(),
    val flashWindowEnd: Long? = null,
    val lastFlashReminderAt: Long? = null,
    val achievements: Map<String, Long> = emptyMap(),
    val couriers: Map<Courier, Long> = emptyMap(),
)

/** Pure: which notifications exist for the current data, and when each is due. */
object NotificationPlanner {
    const val FLASH_LEAD_MILLIS = 10 * 60 * 1_000L
    private const val REVIEW_DELAY_MILLIS = 60 * 60 * 1_000L
    private const val FLASH_CAP_MILLIS = 24 * 60 * 60 * 1_000L

    fun plan(inputs: NotificationInputs): List<PlannedNotification> =
        (inputs.orders.flatMap(::forOrder) + forGames(inputs) + forUnlocks(inputs)).filter { it.deliverAt >= inputs.since }

    private fun forOrder(order: Order): List<PlannedNotification> {
        val link = NotificationLinks.order(order.id)
        val shortId = order.id.take(ORDER_ID_LENGTH).uppercase()
        return when (order.status) {
            OrderStatus.PENDING_SYNC -> emptyList()
            OrderStatus.REJECTED -> listOf(PlannedNotification("order-${order.id}-rejected", NotificationKind.ORDER_REJECTED, order.createdAt + 1, link, listOf(shortId)))
            OrderStatus.CONFIRMED -> {
                val courier = order.courier
                listOf(PlannedNotification("order-${order.id}-confirmed", NotificationKind.ORDER_CONFIRMED, order.createdAt + 1, link, listOf(shortId))) +
                    if (courier == null) emptyList() else forParcels(order, courier, link)
            }
        }
    }

    private fun forParcels(order: Order, courier: Courier, link: String) = order.parcels().flatMapIndexed { index, parcel ->
        val what = parcel.items.first().title + if (parcel.items.size > 1) " +${parcel.items.size - 1}" else ""
        val key = "order-${order.id}-$index"
        listOf(
            PlannedNotification("$key-departed", NotificationKind.COURIER_DEPARTED, parcel.departsAt, link, listOf(courier.name, parcel.origin.city, what)),
            PlannedNotification("$key-delivered", NotificationKind.PARCEL_DELIVERED, parcel.arrivalAt, link, listOf(courier.name, what)),
            PlannedNotification("$key-review", NotificationKind.REVIEW_PROMPT, parcel.arrivalAt + REVIEW_DELAY_MILLIS, link, listOf(parcel.items.first().title)),
        )
    }

    private fun forGames(inputs: NotificationInputs): List<PlannedNotification> {
        val games = inputs.lastPlayed.mapNotNull { (kind, last) ->
            val (type, link) = when (kind) {
                GameKind.SPIN -> NotificationKind.SPIN_READY to NotificationLinks.SPIN
                GameKind.SCRATCH -> NotificationKind.SCRATCH_READY to NotificationLinks.SCRATCH
                else -> return@mapNotNull null
            }
            PlannedNotification("${kind.name.lowercase()}-ready-$last", type, last + kind.cooldownMillis, link)
        }
        val flash = inputs.flashWindowEnd?.let { end ->
            val at = end - FLASH_LEAD_MILLIS
            val last = inputs.lastFlashReminderAt
            if (at > inputs.now && (last == null || at - last >= FLASH_CAP_MILLIS)) {
                PlannedNotification("flash-$end", NotificationKind.FLASH_SALE, at, NotificationLinks.FLASH_SALE)
            } else {
                null
            }
        }
        return games + listOfNotNull(flash)
    }

    private fun forUnlocks(inputs: NotificationInputs): List<PlannedNotification> =
        inputs.achievements.map { (id, at) -> PlannedNotification("achievement-$id", NotificationKind.ACHIEVEMENT, at, NotificationLinks.ACHIEVEMENTS, listOf(id)) } +
            inputs.couriers.filterKeys { !it.isStarter }.map { (courier, at) ->
                PlannedNotification("courier-${courier.name}", NotificationKind.COURIER_UNLOCKED, at, NotificationLinks.GARAGE, listOf(courier.name))
            }

    private const val ORDER_ID_LENGTH = 8
}
