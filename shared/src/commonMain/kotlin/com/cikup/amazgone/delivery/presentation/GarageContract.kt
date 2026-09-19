package com.cikup.amazgone.delivery.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.GeoMath
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.domain.model.Origins

data class GarageState(
    val owned: Set<Courier> = Courier.STARTERS,
    val balance: Long = 0,
    /** Courier awaiting the "unlock for N coins?" confirmation. */
    val confirming: Courier? = null,
    val isBuying: Boolean = false,
    /** Sample trip shown on every card: Apple HQ → the user's last delivery address (or Jakarta). */
    val sampleFrom: String = SAMPLE_ORIGIN_CITY,
    val sampleTo: String = "Jakarta",
    val sampleDestination: GeoPoint = Origins.DEFAULT_DESTINATION,
) : UiState {
    val sampleKm: Double get() = GeoMath.distanceKm(Origins.forStore(SAMPLE_STORE)!!.point, sampleDestination)

    companion object {
        const val SAMPLE_STORE = "official-apple"
        const val SAMPLE_ORIGIN_CITY = "Cupertino"
    }
}

sealed interface GarageIntent : UiIntent {
    data class RequestBuy(val courier: Courier) : GarageIntent
    data object ConfirmBuy : GarageIntent
    data object DismissBuy : GarageIntent
    data object Back : GarageIntent
}

sealed interface GarageEffect : UiEffect {
    data class Unlocked(val courier: Courier) : GarageEffect
    data class NotEnoughCoins(val shortfall: Long) : GarageEffect
    data object NavigateBack : GarageEffect
}
