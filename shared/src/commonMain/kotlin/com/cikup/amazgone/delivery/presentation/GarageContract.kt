package com.cikup.amazgone.delivery.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
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
    val filter: GarageFilter = GarageFilter.ALL,
    /** Set right after an unlock so the screen can celebrate it. */
    val justUnlocked: Courier? = null,
) : UiState {
    val sampleKm: Double get() = GeoMath.distanceKm(Origins.forStore(SAMPLE_STORE)!!.point, sampleDestination)

    /** Sample-trip time for a courier (null = can't make it). */
    fun tripMillis(courier: Courier): Long? = CourierPlan.travelMillis(courier, sampleKm)?.plus(courier.handlingMillis)

    /** Fastest courier the player owns for the sample trip. */
    val fastestOwned: Courier? get() = owned.filter { tripMillis(it) != null }.minByOrNull { tripMillis(it)!! }

    /** Couriers shown for the current filter, grouped by era in timeline order. */
    val sections: List<Pair<CourierEra, List<Courier>>>
        get() = Courier.entries
            .filter {
                when (filter) {
                    GarageFilter.ALL -> true
                    GarageFilter.OWNED -> it in owned
                    GarageFilter.LOCKED -> it !in owned
                }
            }
            .groupBy { CourierEra.of(it) }
            .toList()

    companion object {
        const val SAMPLE_STORE = "official-apple"
        const val SAMPLE_ORIGIN_CITY = "Cupertino"
    }
}

sealed interface GarageIntent : UiIntent {
    data class SetFilter(val filter: GarageFilter) : GarageIntent
    data object CelebrationDone : GarageIntent
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

enum class GarageFilter { ALL, OWNED, LOCKED }

/** Timeline sections. */
enum class CourierEra(val emoji: String, val fromYear: Int) {
    ANCIENT("🏛️", Int.MIN_VALUE),
    STEAM_AND_SAIL("⚙️", 1800),
    MOTOR_AND_JET("🛫", 1900),
    FUTURE("🔭", 2030),
    ;

    companion object {
        fun of(courier: Courier): CourierEra = entries.last { courier.year >= it.fromYear }
    }
}
