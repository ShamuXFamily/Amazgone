package com.cikup.amazgone.orders.presentation.checkout

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.orders.domain.model.point
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.Shipment
import com.cikup.amazgone.orders.domain.model.ShippingAddress

/** ADDRESS only when there is no address yet (or the user taps Change); REVIEW is Amazon's one-page "Place your order". */
enum class CheckoutStep { ADDRESS, REVIEW }

data class CheckoutState(
    val step: CheckoutStep = CheckoutStep.ADDRESS,
    val address: ShippingAddress = ShippingAddress("", "", "", "", ""),
    val invalidFields: Set<String> = emptySet(),
    val summary: CartSummary = CartSummary.EMPTY,
    val coupons: List<Coupon> = emptyList(),
    val selectedCouponCode: String? = null,
    val balance: Long = 0,
    val isPlacing: Boolean = false,
    val error: DomainError? = null,
    val placedOrder: Order? = null,
    val errorPulse: Int = 0,
    /** Courier the user tapped; null = automatically the fastest owned one for this order. */
    val pickedCourier: Courier? = null,
    val ownedCouriers: Set<Courier> = Courier.STARTERS,
    /** Looking the address up on OpenStreetMap (estimates use the country centre meanwhile). */
    val isLocating: Boolean = false,
    /** False until the last order's address has been looked up (avoids flashing the empty form). */
    val addressLoaded: Boolean = false,
    /** A confirmed address exists, so Back from the address form returns to the review page. */
    val hasSavedAddress: Boolean = false,
    /** Clock reading for delivery estimates. */
    val now: Long = 0,
) : UiState {
    val shipments: List<Shipment> get() = CheckoutPlanner.shipments(summary.lines)
    val needsDelivery: Boolean get() = CheckoutPlanner.needsDelivery(shipments)
    val destination: GeoPoint get() = address.point()

    /** Every courier with its arrival window for this order (null window = can't make this trip). */
    val courierOptions: List<CourierOption>
        get() = Courier.entries.map { c ->
            CourierOption(c, c in ownedCouriers, CheckoutPlanner.courierArrival(shipments, destination, c, now))
        }

    /** The picked courier, or the fastest owned one that can make the trip. */
    val courier: Courier
        get() = pickedCourier?.takeIf { it in ownedCouriers }
            ?: courierOptions.filter { it.owned && it.window != null }
                .sortedWith(compareBy<CourierOption> { it.window!!.last }.thenByDescending { it.courier.ordinal }) // ties → newer courier
                .firstOrNull()?.courier
            ?: Courier.PIGEON
    val courierWindow: LongRange? get() = courierOptions.firstOrNull { it.courier == courier }?.window

    /** Couriers are bought once in the Garage, so delivery itself never costs coins. */
    val deliveryFeeCoins: Long get() = 0
    val totalCoins: Long get() = summary.totalCoins
    val balanceAfter: Long get() = balance - totalCoins
    val canAfford: Boolean get() = balanceAfter >= 0
}

sealed interface CheckoutIntent : UiIntent {
    data class FieldChanged(val field: String, val value: String) : CheckoutIntent
    data object Next : CheckoutIntent
    data object Back : CheckoutIntent
    data class SelectCoupon(val code: String?) : CheckoutIntent
    data class SelectCourier(val courier: Courier) : CheckoutIntent
    data object OpenGarage : CheckoutIntent
    data object EditAddress : CheckoutIntent
    data object Pay : CheckoutIntent
    data object ViewOrder : CheckoutIntent
    data object KeepShopping : CheckoutIntent
}

sealed interface CheckoutEffect : UiEffect {
    data object Close : CheckoutEffect
    data class OpenOrder(val orderId: String) : CheckoutEffect
    data object GoHome : CheckoutEffect
    data object OpenGarage : CheckoutEffect
}

data class CourierOption(val courier: Courier, val owned: Boolean, val window: LongRange?)
