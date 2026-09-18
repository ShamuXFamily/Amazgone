package com.cikup.amazgone.orders.presentation.checkout

import com.cikup.amazgone.account.domain.usecase.ObserveSessionUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCouponsUseCase
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.orders.domain.model.AddressValidator
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.usecase.PlaceOrderUseCase
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class CheckoutViewModel(
    observeCart: ObserveCartUseCase,
    observeCoupons: ObserveCouponsUseCase,
    observeWallet: ObserveWalletUseCase,
    observeSession: ObserveSessionUseCase,
    private val placeOrder: PlaceOrderUseCase,
) : MviViewModel<CheckoutState, CheckoutIntent, CheckoutEffect>(CheckoutState()) {

    private val selectedCode = MutableStateFlow<String?>(null)

    init {
        val coupons = observeCoupons()
        val selectedCoupon = combine(coupons, selectedCode) { all, code -> all.firstOrNull { it.code == code } }
        coupons.observe { setState { copy(coupons = it) } }
        observeCart(selectedCoupon).observe { summary ->
            // keep the last non-empty summary so the success screen is not replaced by an empty cart
            if (!summary.isEmpty || currentState.placedOrder == null) setState { copy(summary = summary) }
        }
        observeWallet().observe { setState { copy(balance = it.coins) } }
        observeSession().observe { session ->
            if (session != null && currentState.address.fullName.isEmpty()) {
                setState { copy(address = address.copy(fullName = session.username)) }
            }
        }
    }

    override fun handleIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.FieldChanged -> setState {
                copy(address = address.with(intent.field, intent.value), invalidFields = invalidFields - intent.field)
            }
            CheckoutIntent.Next -> next()
            CheckoutIntent.Back -> back()
            is CheckoutIntent.SelectCoupon -> {
                selectedCode.value = intent.code
                setState { copy(selectedCouponCode = intent.code) }
            }
            CheckoutIntent.Pay -> pay()
            CheckoutIntent.ViewOrder -> currentState.placedOrder?.let { sendEffect(CheckoutEffect.OpenOrder(it.id)) }
            CheckoutIntent.KeepShopping -> sendEffect(CheckoutEffect.GoHome)
        }
    }

    private fun next() {
        when (currentState.step) {
            CheckoutStep.ADDRESS -> {
                val invalid = AddressValidator.invalidFields(currentState.address)
                if (invalid.isEmpty()) {
                    setState { copy(step = CheckoutStep.PAYMENT) }
                } else {
                    setState { copy(invalidFields = invalid, errorPulse = errorPulse + 1) }
                }
            }
            CheckoutStep.PAYMENT -> setState { copy(step = CheckoutStep.REVIEW) }
            CheckoutStep.REVIEW -> pay()
        }
    }

    private fun back() {
        when (currentState.step) {
            CheckoutStep.ADDRESS -> sendEffect(CheckoutEffect.Close)
            CheckoutStep.PAYMENT -> setState { copy(step = CheckoutStep.ADDRESS) }
            CheckoutStep.REVIEW -> setState { copy(step = CheckoutStep.PAYMENT) }
        }
    }

    private fun pay() {
        if (currentState.isPlacing || currentState.placedOrder != null) return
        setState { copy(isPlacing = true, error = null) }
        launchSafely {
            when (val result = placeOrder(currentState.summary, currentState.address)) {
                is DomainResult.Success -> setState { copy(isPlacing = false, placedOrder = result.value) }
                is DomainResult.Failure -> setState { copy(isPlacing = false, error = result.error, errorPulse = errorPulse + 1) }
            }
        }
    }

    override fun onError(throwable: Throwable) = setState { copy(isPlacing = false) }
}

private fun ShippingAddress.with(field: String, value: String) = when (field) {
    AddressValidator.FIELD_NAME -> copy(fullName = value)
    AddressValidator.FIELD_LINE1 -> copy(line1 = value)
    AddressValidator.FIELD_CITY -> copy(city = value)
    AddressValidator.FIELD_POSTAL -> copy(postalCode = value)
    AddressValidator.FIELD_COUNTRY -> copy(country = value)
    else -> this
}
