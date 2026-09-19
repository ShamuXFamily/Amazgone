package com.cikup.amazgone.orders.domain.model

import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
import com.cikup.amazgone.delivery.domain.model.GeoMath
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.domain.model.Origin
import com.cikup.amazgone.delivery.domain.model.Origins
import com.cikup.amazgone.stores.domain.model.StoreDirectory
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

    /**
     * Earliest..latest arrival of the parcels if [courier] carries them to [destination], or null when it
     * can't (a drone can't cross an ocean). Digital-only orders need no courier: empty window at [nowMillis].
     */
    fun courierArrival(shipments: List<Shipment>, destination: GeoPoint, courier: Courier, nowMillis: Long): LongRange? {
        val etas = shipments.filter { !it.isDigital }.map { shipment ->
            val origin = Origins.forStore(shipment.store.id) ?: return@map nowMillis
            CourierPlan.arrivalAt(nowMillis, courier, GeoMath.distanceKm(origin.point, destination)) ?: return null
        }
        return if (etas.isEmpty()) nowMillis..nowMillis else etas.min()..etas.max()
    }

    /** Longest single trip, e.g. "Cupertino → Jakarta · 13,900 km". */
    fun longestTrip(shipments: List<Shipment>, destination: GeoPoint): Pair<Origin, Double>? =
        shipments.filter { !it.isDigital }.mapNotNull { s -> Origins.forStore(s.store.id)?.let { it to GeoMath.distanceKm(it.point, destination) } }
            .maxByOrNull { it.second }

    /** Earliest..latest arrival (epoch millis) for parcels ordered at [nowMillis]. */
    fun arrival(nowMillis: Long, option: DeliveryOption): LongRange =
        nowMillis + option.minDays * DAY_MILLIS..nowMillis + option.maxDays * DAY_MILLIS
}

/** A placed order's items per seller, for the confirmation and order pages. */
data class PlacedShipment(val storeName: String?, val digital: Boolean, val items: List<OrderItem>)

fun Order.shipments(): List<PlacedShipment> =
    items.groupBy { it.storeName to it.digital }.map { (key, grouped) -> PlacedShipment(key.first, key.second, grouped) }

/** Arrival window for the parcels of this order; null when everything is digital. */
fun Order.arrival(): LongRange? {
    if (courier != null) {
        val parcels = parcels()
        return if (parcels.isEmpty()) null else parcels.minOf { it.arrivalAt }..parcels.maxOf { it.arrivalAt }
    }
    return if (items.all { it.digital }) null else CheckoutPlanner.arrival(createdAt, delivery)
}

/** One store's parcel on its way: where from, where to, and when it lands. */
data class Parcel(
    val storeName: String?,
    val origin: Origin,
    val destination: GeoPoint,
    val items: List<OrderItem>,
    val distanceKm: Double,
    val departsAt: Long,
    val arrivalAt: Long,
)

/** Map position of an address: geocoded point, else the country's centre, else the warehouse city. */
fun ShippingAddress.point(): GeoPoint {
    val latitude = lat
    val longitude = lon
    return if (latitude != null && longitude != null) GeoPoint(latitude, longitude)
    else Origins.countryCentre(country) ?: Origins.DEFAULT_DESTINATION
}

/** Courier orders: one parcel per physical seller, each flying its own great-circle route. */
fun Order.parcels(): List<Parcel> {
    val carrier = courier ?: return emptyList()
    val destination = address.point()
    return items.filter { !it.digital }.groupBy { it.storeId ?: StoreDirectory.AMAZGONE.id }.mapNotNull { (storeId, lines) ->
        val origin = Origins.forStore(storeId) ?: return@mapNotNull null
        val km = GeoMath.distanceKm(origin.point, destination)
        val departs = createdAt + carrier.handlingMillis
        val eta = CourierPlan.arrivalAt(createdAt, carrier, km) ?: departs
        Parcel(lines.first().storeName, origin, destination, lines, km, departs, eta)
    }
}

/** Where an order is in its journey (what the tracker shows). */
enum class OrderStage { PLACED, CONFIRMED, SHIPPED, DELIVERED, REJECTED }

/** Parcels leave the warehouse this long after the order is placed. */
const val SHIP_AFTER_MILLIS = 12 * 60 * 60 * 1_000L

/**
 * Derived from time, never stored: unsynced orders stay PLACED, digital-only orders are delivered as soon as
 * the server confirms them, parcels are delivered on the first arrival day or when the customer confirms receipt.
 */
fun Order.stageAt(nowMillis: Long): OrderStage {
    val arrival = arrival()
    return when {
        status == OrderStatus.REJECTED -> OrderStage.REJECTED
        status == OrderStatus.PENDING_SYNC -> OrderStage.PLACED
        courier != null -> courierStage(nowMillis)
        deliveredAt != null || arrival == null || nowMillis >= arrival.first -> OrderStage.DELIVERED
        nowMillis >= createdAt + SHIP_AFTER_MILLIS -> OrderStage.SHIPPED
        else -> OrderStage.CONFIRMED
    }
}

/** Packing, then in transit until the last parcel lands (there is no early "received" for real couriers). */
private fun Order.courierStage(nowMillis: Long): OrderStage {
    val parcels = parcels()
    return when {
        parcels.isEmpty() || nowMillis >= parcels.maxOf { it.arrivalAt } -> OrderStage.DELIVERED
        nowMillis >= createdAt + (courier?.handlingMillis ?: CourierPlan.HANDLING_MILLIS) -> OrderStage.SHIPPED
        else -> OrderStage.CONFIRMED
    }
}

/** Digital items arrive the moment the server confirms the order; a parcel when its courier lands. */
fun Order.isItemDelivered(item: OrderItem, nowMillis: Long): Boolean {
    val stage = stageAt(nowMillis)
    if (stage == OrderStage.PLACED || stage == OrderStage.REJECTED) return false
    if (stage == OrderStage.DELIVERED || item.digital) return true
    val parcel = parcels().firstOrNull { item in it.items } ?: return false
    return nowMillis >= parcel.arrivalAt
}

/** "Order received" is offered while a confirmed parcel is still on its way. */
fun Order.canConfirmReceived(nowMillis: Long): Boolean =
    courier == null && stageAt(nowMillis).let { it == OrderStage.CONFIRMED || it == OrderStage.SHIPPED }
