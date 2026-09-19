package com.cikup.amazgone.orders.domain.model

import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.stores.domain.model.Store
import com.cikup.amazgone.stores.domain.model.StoreKind

/** Delivery speed for the physical part of an order. Fees are in coins (10 coins ≈ 1 USD). */
enum class DeliveryOption(val feeCoins: Long, val minDays: Int, val maxDays: Int) {
    STANDARD(feeCoins = 0, minDays = 3, maxDays = 5),
    EXPRESS(feeCoins = 50, minDays = 1, maxDays = 1),
    ;

    companion object {
        fun parse(value: String?): DeliveryOption = entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

/** Everything one store sends; digital stores deliver instantly (a code, no parcel). */
data class Shipment(val store: Store, val lines: List<CartLine>) {
    val isDigital: Boolean get() = store.kind == StoreKind.DIGITAL
    val subtotalCoins: Long get() = lines.sumOf { it.lineTotalCoins }
}

/** Pure checkout rules, Amazon-style: one shipment per seller, one delivery speed for the parcels. */
object CheckoutPlanner {
    private const val DAY_MILLIS = 24 * 60 * 60 * 1_000L

    /** Keeps the cart's order: stores appear in the order their first item was added. */
    fun shipments(lines: List<CartLine>): List<Shipment> =
        lines.groupBy { it.product.store }.map { (store, storeLines) -> Shipment(store, storeLines) }

    fun needsDelivery(shipments: List<Shipment>): Boolean = shipments.any { !it.isDigital }

    fun deliveryFee(shipments: List<Shipment>, option: DeliveryOption): Long =
        if (needsDelivery(shipments)) option.feeCoins else 0

    /** Earliest..latest arrival (epoch millis) for parcels ordered at [nowMillis]. */
    fun arrival(nowMillis: Long, option: DeliveryOption): LongRange =
        nowMillis + option.minDays * DAY_MILLIS..nowMillis + option.maxDays * DAY_MILLIS
}

/** A placed order's items per seller, for the confirmation and order pages. */
data class PlacedShipment(val storeName: String?, val digital: Boolean, val items: List<OrderItem>)

fun Order.shipments(): List<PlacedShipment> =
    items.groupBy { it.storeName to it.digital }.map { (key, grouped) -> PlacedShipment(key.first, key.second, grouped) }

/** Arrival window for the parcels of this order; null when everything is digital. */
fun Order.arrival(): LongRange? =
    if (items.all { it.digital }) null else CheckoutPlanner.arrival(createdAt, delivery)
