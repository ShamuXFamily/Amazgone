package com.cikup.amazgone.orders.presentation.checkout

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.ShippingAddress

enum class CheckoutStep { ADDRESS, PAYMENT, REVIEW }

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
) : UiState {
    val balanceAfter: Long get() = balance - summary.totalCoins
    val canAfford: Boolean get() = balanceAfter >= 0
}

sealed interface CheckoutIntent : UiIntent {
    data class FieldChanged(val field: String, val value: String) : CheckoutIntent
    data object Next : CheckoutIntent
    data object Back : CheckoutIntent
    data class SelectCoupon(val code: String?) : CheckoutIntent
    data object Pay : CheckoutIntent
    data object ViewOrder : CheckoutIntent
    data object KeepShopping : CheckoutIntent
}

sealed interface CheckoutEffect : UiEffect {
    data object Close : CheckoutEffect
    data class OpenOrder(val orderId: String) : CheckoutEffect
    data object GoHome : CheckoutEffect
}
