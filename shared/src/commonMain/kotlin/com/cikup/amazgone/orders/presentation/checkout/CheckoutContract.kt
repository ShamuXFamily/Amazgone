package com.cikup.amazgone.orders.presentation.checkout

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.orders.domain.model.DeliveryOption
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
    val delivery: DeliveryOption = DeliveryOption.STANDARD,
    /** False until the last order's address has been looked up (avoids flashing the empty form). */
    val addressLoaded: Boolean = false,
    /** A confirmed address exists, so Back from the address form returns to the review page. */
    val hasSavedAddress: Boolean = false,
    /** Clock reading for delivery estimates. */
    val now: Long = 0,
) : UiState {
    val shipments: List<Shipment> get() = CheckoutPlanner.shipments(summary.lines)
    val needsDelivery: Boolean get() = CheckoutPlanner.needsDelivery(shipments)
    val deliveryFeeCoins: Long get() = CheckoutPlanner.deliveryFee(shipments, delivery)
    val totalCoins: Long get() = summary.totalCoins + deliveryFeeCoins
    val balanceAfter: Long get() = balance - totalCoins
    val canAfford: Boolean get() = balanceAfter >= 0
}

sealed interface CheckoutIntent : UiIntent {
    data class FieldChanged(val field: String, val value: String) : CheckoutIntent
    data object Next : CheckoutIntent
    data object Back : CheckoutIntent
    data class SelectCoupon(val code: String?) : CheckoutIntent
    data class SelectDelivery(val option: DeliveryOption) : CheckoutIntent
    data object EditAddress : CheckoutIntent
    data object Pay : CheckoutIntent
    data object ViewOrder : CheckoutIntent
    data object KeepShopping : CheckoutIntent
}

sealed interface CheckoutEffect : UiEffect {
    data object Close : CheckoutEffect
    data class OpenOrder(val orderId: String) : CheckoutEffect
    data object GoHome : CheckoutEffect
}
