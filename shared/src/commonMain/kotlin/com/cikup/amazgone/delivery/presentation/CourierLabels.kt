package com.cikup.amazgone.delivery.presentation

import amazgone.shared.generated.resources.*
import androidx.compose.runtime.Composable
import com.cikup.amazgone.delivery.domain.model.Courier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class CourierText(val name: StringResource, val era: StringResource, val fact: StringResource)

private fun Courier.text(): CourierText = when (this) {
    Courier.CAMEL -> CourierText(Res.string.courier_CAMEL_name, Res.string.courier_CAMEL_era, Res.string.courier_CAMEL_fact)
    Courier.PIGEON -> CourierText(Res.string.courier_PIGEON_name, Res.string.courier_PIGEON_era, Res.string.courier_PIGEON_fact)
    Courier.RELAY_RUNNER -> CourierText(Res.string.courier_RELAY_RUNNER_name, Res.string.courier_RELAY_RUNNER_era, Res.string.courier_RELAY_RUNNER_fact)
    Courier.STEAM_TRAIN -> CourierText(Res.string.courier_STEAM_TRAIN_name, Res.string.courier_STEAM_TRAIN_era, Res.string.courier_STEAM_TRAIN_fact)
    Courier.CLIPPER_SHIP -> CourierText(Res.string.courier_CLIPPER_SHIP_name, Res.string.courier_CLIPPER_SHIP_era, Res.string.courier_CLIPPER_SHIP_fact)
    Courier.PONY_EXPRESS -> CourierText(Res.string.courier_PONY_EXPRESS_name, Res.string.courier_PONY_EXPRESS_era, Res.string.courier_PONY_EXPRESS_fact)
    Courier.MAIL_TRUCK -> CourierText(Res.string.courier_MAIL_TRUCK_name, Res.string.courier_MAIL_TRUCK_era, Res.string.courier_MAIL_TRUCK_fact)
    Courier.AIRMAIL_BIPLANE -> CourierText(Res.string.courier_AIRMAIL_BIPLANE_name, Res.string.courier_AIRMAIL_BIPLANE_era, Res.string.courier_AIRMAIL_BIPLANE_fact)
    Courier.CARGO_JET -> CourierText(Res.string.courier_CARGO_JET_name, Res.string.courier_CARGO_JET_era, Res.string.courier_CARGO_JET_fact)
    Courier.DRONE -> CourierText(Res.string.courier_DRONE_name, Res.string.courier_DRONE_era, Res.string.courier_DRONE_fact)
    Courier.ROCKET -> CourierText(Res.string.courier_ROCKET_name, Res.string.courier_ROCKET_era, Res.string.courier_ROCKET_fact)
    Courier.TELEPORTER -> CourierText(Res.string.courier_TELEPORTER_name, Res.string.courier_TELEPORTER_era, Res.string.courier_TELEPORTER_fact)
}

@Composable
fun courierName(courier: Courier): String = stringResource(courier.text().name)

/** For non-composable contexts (snackbar text via getString). */
fun courierNameRes(courier: Courier): StringResource = courier.text().name

@Composable
fun courierEra(courier: Courier): String = stringResource(courier.text().era)

@Composable
fun courierFact(courier: Courier): String = stringResource(courier.text().fact)
