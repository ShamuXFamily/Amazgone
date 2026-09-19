package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_courier_arrives
import amazgone.shared.generated.resources.checkout_courier_cant
import amazgone.shared.generated.resources.checkout_courier_garage
import amazgone.shared.generated.resources.checkout_courier_title
import amazgone.shared.generated.resources.checkout_courier_unlock
import amazgone.shared.generated.resources.checkout_locating
import amazgone.shared.generated.resources.checkout_trip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import com.cikup.amazgone.delivery.domain.model.Origins
import com.cikup.amazgone.delivery.presentation.courierName
import com.cikup.amazgone.delivery.presentation.map.DeliveryMap
import com.cikup.amazgone.delivery.presentation.map.MapRoute
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import org.jetbrains.compose.resources.stringResource

/** Owned couriers first (fastest first), then locked ones with their unlock price. */
@Composable
fun CourierPicker(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit, modifier: Modifier = Modifier) {
    val options = state.courierOptions.sortedWith(
        compareByDescending<CourierOption> { it.owned }.thenBy { it.window?.last ?: Long.MAX_VALUE },
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.checkout_courier_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { onIntent(CheckoutIntent.OpenGarage) }) {
                Icon(Icons.Rounded.Warehouse, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.checkout_courier_garage), color = AmazgoneTheme.extended.cta, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
            }
        }
        CheckoutPlanner.longestTrip(state.shipments, state.destination)?.let { (origin, km) ->
            Text(
                if (state.isLocating) stringResource(Res.string.checkout_locating)
                else stringResource(Res.string.checkout_trip, origin.city, state.address.city.ifBlank { state.address.country }, Formatters.coins(km.toLong())),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val origins = state.shipments.filter { !it.isDigital }.mapNotNull { Origins.forStore(it.store.id)?.point }.distinct()
        if (origins.isNotEmpty()) {
            DeliveryMap(
                routes = origins.map { MapRoute(it, progress = 0.0, emoji = state.courier.emoji) },
                home = state.destination,
                modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), contentPadding = PaddingValues(end = AmazgoneDimens.spaceLg)) {
            items(options, key = { it.courier.name }) { option ->
                CourierCard(option, selected = option.courier == state.courier, now = state.now) { onIntent(CheckoutIntent.SelectCourier(option.courier)) }
            }
        }
    }
}

@Composable
private fun CourierCard(option: CourierOption, selected: Boolean, now: Long, onClick: () -> Unit) {
    val cta = AmazgoneTheme.extended.cta
    val border by animateColorAsState(if (selected) cta else MaterialTheme.colorScheme.outlineVariant, label = "courierBorder")
    val width by animateDpAsState(if (selected) AmazgoneDimens.spaceXs / 2 else AmazgoneDimens.spaceXs / 4, MotionTokens.bouncy(), label = "courierWidth")
    val usable = option.owned && option.window != null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(width, border),
        modifier = Modifier.width(CARD_WIDTH).clip(MaterialTheme.shapes.large)
            .selectable(selected, enabled = option.window != null || !option.owned, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(
            Modifier.padding(AmazgoneDimens.spaceMd).alpha(if (usable) 1f else LOCKED_ALPHA),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(option.courier.emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (!option.owned) Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
            }
            Text(courierName(option.courier), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val window = option.window
            Text(
                when {
                    !option.owned -> stringResource(Res.string.checkout_courier_unlock, Formatters.coins(option.courier.priceCoins))
                    window == null -> stringResource(Res.string.checkout_courier_cant)
                    else -> stringResource(Res.string.checkout_courier_arrives, Formatters.weekdayDate(window.last), durationText(window.last - now))
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (option.owned && window != null) AmazgoneTheme.extended.success else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private val CARD_WIDTH = AmazgoneDimens.iconXl * 1.45f
private val PREVIEW_HEIGHT = AmazgoneDimens.iconXl * 1.7f
private const val LOCKED_ALPHA = 0.55f
