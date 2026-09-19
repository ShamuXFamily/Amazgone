package com.cikup.amazgone.delivery.presentation

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.usecase.BuyCourierUseCase
import com.cikup.amazgone.delivery.domain.usecase.ObserveOwnedCouriersUseCase
import com.cikup.amazgone.orders.domain.model.point
import com.cikup.amazgone.orders.domain.usecase.ObserveOrdersUseCase
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase

class GarageViewModel(
    observeOwned: ObserveOwnedCouriersUseCase,
    observeWallet: ObserveWalletUseCase,
    observeOrders: ObserveOrdersUseCase,
    private val buyCourier: BuyCourierUseCase,
    private val analytics: Analytics,
) : MviViewModel<GarageState, GarageIntent, GarageEffect>(GarageState()) {

    init {
        observeOwned().observe { setState { copy(owned = it) } }
        observeWallet(recentLimit = 1).observe { setState { copy(balance = it.coins) } }
        observeOrders().observe { orders ->
            orders.firstOrNull()?.address?.let { address ->
                setState { copy(sampleTo = address.city.ifBlank { address.country }, sampleDestination = address.point()) }
            }
        }
    }

    override fun handleIntent(intent: GarageIntent) {
        when (intent) {
            is GarageIntent.RequestBuy -> requestBuy(intent.courier)
            GarageIntent.ConfirmBuy -> confirm()
            GarageIntent.DismissBuy -> setState { copy(confirming = null) }
            GarageIntent.Back -> sendEffect(GarageEffect.NavigateBack)
        }
    }

    private fun requestBuy(courier: Courier) {
        if (courier in currentState.owned) return
        val shortfall = courier.priceCoins - currentState.balance
        if (shortfall > 0) return sendEffect(GarageEffect.NotEnoughCoins(shortfall))
        setState { copy(confirming = courier) }
    }

    private fun confirm() {
        val courier = currentState.confirming ?: return
        if (currentState.isBuying) return
        setState { copy(isBuying = true) }
        launchSafely {
            when (val result = buyCourier(courier)) {
                is DomainResult.Success -> {
                    analytics.spendCoins(courier.priceCoins, "courier_${courier.name.lowercase()}")
                    setState { copy(isBuying = false, confirming = null) }
                    sendEffect(GarageEffect.Unlocked(courier))
                }
                is DomainResult.Failure -> {
                    setState { copy(isBuying = false, confirming = null) }
                    val error = result.error
                    if (error is DomainError.InsufficientCoins) sendEffect(GarageEffect.NotEnoughCoins(error.required - error.available))
                }
            }
        }
    }

    override fun onError(throwable: Throwable) = setState { copy(isBuying = false) }
}
