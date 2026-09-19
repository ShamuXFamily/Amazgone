package com.cikup.amazgone.delivery.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.garage_owned
import amazgone.shared.generated.resources.garage_range
import amazgone.shared.generated.resources.garage_speed_day
import amazgone.shared.generated.resources.garage_speed_flat
import amazgone.shared.generated.resources.garage_starter
import amazgone.shared.generated.resources.garage_trip_cant
import amazgone.shared.generated.resources.garage_trip_time
import amazgone.shared.generated.resources.garage_unlock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
import org.jetbrains.compose.resources.stringResource

/** One stop on the history timeline: era dot + line on the left, the courier card on the right. */
@Composable
fun CourierTimelineRow(courier: Courier, state: GarageState, isLast: Boolean, onIntent: (GarageIntent) -> Unit, modifier: Modifier = Modifier) {
    val owned = courier in state.owned
    val ext = AmazgoneTheme.extended
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = AmazgoneDimens.spaceLg).size(DOT).background(if (owned) ext.cta else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            if (!isLast) Box(Modifier.width(LINE).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f).padding(bottom = AmazgoneDimens.spaceXs)) {
            Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    Surface(color = (if (owned) ext.cta else MaterialTheme.colorScheme.outline).copy(alpha = TINT), shape = CircleShape) {
                        Text(courier.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(AmazgoneDimens.spaceSm))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(courierName(courier), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(courierEra(courier), style = MaterialTheme.typography.labelMedium, color = ext.cta)
                    }
                }
                Text(courierFact(courier), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpeedLine(courier)
                SampleTrip(courier, state)
                BuyButton(courier, owned, onIntent)
            }
        }
    }
}

@Composable
private fun SpeedLine(courier: Courier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
        val flat = courier.flatMillis
        Text(
            listOfNotNull(
                if (flat != null) stringResource(Res.string.garage_speed_flat, durationText(flat))
                else stringResource(Res.string.garage_speed_day, Formatters.coins(courier.kmPerDay.toLong())),
                courier.maxRangeKm?.let { stringResource(Res.string.garage_range, Formatters.coins(it.toLong())) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** How long this courier would take on the sample trip (to the user's own address when known). */
@Composable
private fun SampleTrip(courier: Courier, state: GarageState) {
    val travel = CourierPlan.travelMillis(courier, state.sampleKm)
    Text(
        if (travel == null) stringResource(Res.string.garage_trip_cant, state.sampleFrom, state.sampleTo)
        else stringResource(Res.string.garage_trip_time, state.sampleFrom, state.sampleTo, durationText(travel + courier.handlingMillis)),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (travel == null) MaterialTheme.colorScheme.error else AmazgoneTheme.extended.success,
    )
}

@Composable
private fun BuyButton(courier: Courier, owned: Boolean, onIntent: (GarageIntent) -> Unit) {
    val ext = AmazgoneTheme.extended
    if (owned) {
        Row(Modifier.padding(top = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ext.success, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(stringResource(if (courier.isStarter) Res.string.garage_starter else Res.string.garage_owned), style = MaterialTheme.typography.labelLarge, color = ext.success, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = { onIntent(GarageIntent.RequestBuy(courier)) },
            colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta),
            modifier = Modifier.padding(top = AmazgoneDimens.spaceXs),
        ) { Text(stringResource(Res.string.garage_unlock, Formatters.coins(courier.priceCoins))) }
    }
}

private val DOT = AmazgoneDimens.spaceMd
private val LINE = AmazgoneDimens.spaceXs / 2
private const val TINT = 0.15f
