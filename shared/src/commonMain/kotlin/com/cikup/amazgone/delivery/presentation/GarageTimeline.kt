package com.cikup.amazgone.delivery.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.era_ANCIENT
import amazgone.shared.generated.resources.era_ANCIENT_years
import amazgone.shared.generated.resources.era_FUTURE
import amazgone.shared.generated.resources.era_FUTURE_years
import amazgone.shared.generated.resources.era_MOTOR_AND_JET
import amazgone.shared.generated.resources.era_MOTOR_AND_JET_years
import amazgone.shared.generated.resources.era_STEAM_AND_SAIL
import amazgone.shared.generated.resources.era_STEAM_AND_SAIL_years
import amazgone.shared.generated.resources.garage_in_fleet
import amazgone.shared.generated.resources.garage_need
import amazgone.shared.generated.resources.garage_range
import amazgone.shared.generated.resources.garage_speed
import amazgone.shared.generated.resources.garage_speed_day
import amazgone.shared.generated.resources.garage_speed_flat
import amazgone.shared.generated.resources.garage_starter
import amazgone.shared.generated.resources.garage_trip_cant
import amazgone.shared.generated.resources.garage_unlock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import com.cikup.amazgone.delivery.domain.model.Courier
import org.jetbrains.compose.resources.stringResource
import kotlin.math.log10

/** Each era gets one of the palette's tile accents so the timeline reads as chapters. */
@Composable
private fun eraColor(era: CourierEra): Color = AmazgoneTheme.extended.tileIcons.let {
    when (era) {
        CourierEra.ANCIENT -> it[TINT_GOLD]
        CourierEra.STEAM_AND_SAIL -> it[TINT_GREEN]
        CourierEra.MOTOR_AND_JET -> it[TINT_BLUE]
        CourierEra.FUTURE -> it[TINT_PURPLE]
    }
}

@Composable
fun EraHeader(era: CourierEra, modifier: Modifier = Modifier) {
    val (title, years) = when (era) {
        CourierEra.ANCIENT -> Res.string.era_ANCIENT to Res.string.era_ANCIENT_years
        CourierEra.STEAM_AND_SAIL -> Res.string.era_STEAM_AND_SAIL to Res.string.era_STEAM_AND_SAIL_years
        CourierEra.MOTOR_AND_JET -> Res.string.era_MOTOR_AND_JET to Res.string.era_MOTOR_AND_JET_years
        CourierEra.FUTURE -> Res.string.era_FUTURE to Res.string.era_FUTURE_years
    }
    val color = eraColor(era)
    Row(modifier.padding(top = AmazgoneDimens.spaceMd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Surface(color = color.copy(alpha = TINT), shape = CircleShape) {
            Text(era.emoji, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(AmazgoneDimens.spaceSm))
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(years), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

/** Timeline stop: era-coloured dot + line on the left, the courier card on the right. */
@Composable
fun CourierCard(courier: Courier, state: GarageState, isLastInEra: Boolean, onIntent: (GarageIntent) -> Unit, modifier: Modifier = Modifier) {
    val era = CourierEra.of(courier)
    val color = eraColor(era)
    val owned = courier in state.owned
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Column(Modifier.fillMaxHeight().width(DOT), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = AmazgoneDimens.spaceXl).size(DOT).background(if (owned) color else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            if (!isLastInEra) Box(Modifier.width(LINE).weight(1f).background(color.copy(alpha = LINE_ALPHA)))
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    Surface(color = color.copy(alpha = TINT), shape = CircleShape, modifier = Modifier.size(EMOJI_DISC)) {
                        Box(contentAlignment = Alignment.Center) { Text(courier.emoji, style = MaterialTheme.typography.headlineMedium) }
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(courierName(courier), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusBadge(courier, owned)
                        }
                        Text(courierEra(courier), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(courierFact(courier), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpeedMeter(courier, state, color)
                TripPill(courier, state)
                if (!owned) UnlockButton(courier, state.balance, onIntent)
            }
        }
    }
}

@Composable
private fun StatusBadge(courier: Courier, owned: Boolean) {
    val ext = AmazgoneTheme.extended
    val (tint, icon) = if (owned) ext.success to Icons.Rounded.CheckCircle else MaterialTheme.colorScheme.onSurfaceVariant to Icons.Rounded.Lock
    Surface(color = tint.copy(alpha = TINT), contentColor = tint, shape = CircleShape) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
            if (owned) {
                Text(
                    stringResource(if (courier.isStarter) Res.string.garage_starter else Res.string.garage_in_fleet),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = AmazgoneDimens.spaceXs),
                )
            }
        }
    }
}

/** Log-scale bar: camel ≈ empty, teleporter full, so the leap from sail to jet is visible. */
@Composable
private fun SpeedMeter(courier: Courier, state: GarageState, color: Color) {
    val trip = state.tripMillis(courier)
    val kmPerDay = if (trip != null && trip > 0) state.sampleKm / (trip / DAY_MILLIS) else courier.kmPerDay
    val target = ((log10(kmPerDay.coerceAtLeast(MIN_SPEED)) - LOG_MIN) / (LOG_MAX - LOG_MIN)).toFloat().coerceIn(MIN_FILL, 1f)
    val fill by animateFloatAsState(target, MotionTokens.gentle(), label = "speed")
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.garage_speed), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val flat = courier.flatMillis
            Text(
                listOfNotNull(
                    if (flat != null) stringResource(Res.string.garage_speed_flat, durationText(flat))
                    else stringResource(Res.string.garage_speed_day, Formatters.coins(courier.kmPerDay.toLong())),
                    courier.maxRangeKm?.let { stringResource(Res.string.garage_range, Formatters.coins(it.toLong())) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.fillMaxWidth().height(AmazgoneDimens.spaceSm).clip(CircleShape).background(color.copy(alpha = TINT))) {
            Box(Modifier.fillMaxWidth(fill).height(AmazgoneDimens.spaceSm).clip(CircleShape).background(color))
        }
    }
}

/** "📍 Cupertino → Jakarta   1 day" — the sample trip, to the user's own address when known. */
@Composable
private fun TripPill(courier: Courier, state: GarageState) {
    val trip = state.tripMillis(courier)
    val ext = AmazgoneTheme.extended
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Place, contentDescription = null, tint = ext.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
            if (trip == null) {
                Text(stringResource(Res.string.garage_trip_cant, state.sampleFrom, state.sampleTo), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
            } else {
                Text("${state.sampleFrom} → ${state.sampleTo}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f).padding(start = AmazgoneDimens.spaceXs))
                Text(durationText(trip), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ext.success)
            }
        }
    }
}

@Composable
private fun UnlockButton(courier: Courier, balance: Long, onIntent: (GarageIntent) -> Unit) {
    val ext = AmazgoneTheme.extended
    val shortfall = courier.priceCoins - balance
    val modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget)
    if (shortfall <= 0) {
        Button(onClick = { onIntent(GarageIntent.RequestBuy(courier)) }, colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta), modifier = modifier) {
            Text(stringResource(Res.string.garage_unlock), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(end = AmazgoneDimens.spaceSm))
            CoinAmount(courier.priceCoins, style = MaterialTheme.typography.titleSmall.copy(color = ext.onCta, fontWeight = FontWeight.Bold))
        }
    } else {
        // Still tappable: the ViewModel answers with a "play games to earn more" message.
        OutlinedButton(onClick = { onIntent(GarageIntent.RequestBuy(courier)) }, modifier = modifier) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(
                stringResource(Res.string.garage_need, Formatters.coins(shortfall)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = AmazgoneDimens.spaceXs),
            )
        }
    }
}

private val DOT = AmazgoneDimens.spaceMd
private val LINE = AmazgoneDimens.spaceXs / 2
private val EMOJI_DISC = AmazgoneDimens.iconLg * 1.15f
private const val TINT = 0.15f
private const val LINE_ALPHA = 0.4f
private const val DAY_MILLIS = 24 * 60 * 60 * 1_000.0
private const val MIN_SPEED = 40.0
private const val LOG_MIN = 1.6 // log10(40 km/day, the camel)
private const val LOG_MAX = 7.3 // log10(~20 million km/day, the teleporter on a 14,000 km trip)
private const val MIN_FILL = 0.04f
private const val TINT_BLUE = 0
private const val TINT_GREEN = 2
private const val TINT_PURPLE = 3
private const val TINT_GOLD = 4
