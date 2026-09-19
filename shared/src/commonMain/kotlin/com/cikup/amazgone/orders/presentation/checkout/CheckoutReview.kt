package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_arrives_on
import amazgone.shared.generated.resources.checkout_change
import amazgone.shared.generated.resources.checkout_deliver_to
import amazgone.shared.generated.resources.checkout_instant
import amazgone.shared.generated.resources.checkout_qty
import amazgone.shared.generated.resources.checkout_shipment
import amazgone.shared.generated.resources.store_sold_by
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import amazgone.shared.generated.resources.checkout_courier_arrives
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.orders.domain.model.Shipment
import com.cikup.amazgone.stores.presentation.StoreLogo
import com.cikup.amazgone.stores.presentation.VerifiedIcon
import org.jetbrains.compose.resources.stringResource

/** Amazon's one-page "Place your order": address, speed, payment, one card per shipment, summary. */
@Composable
fun ReviewStep(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
    ) {
        DeliverToCard(state) { onIntent(CheckoutIntent.EditAddress) }
        if (state.needsDelivery) CourierPicker(state, onIntent, Modifier.staggeredEnter(1))
        PaymentSection(state, onIntent, Modifier.staggeredEnter(2))
        state.shipments.forEachIndexed { index, shipment ->
            ShipmentCard(shipment, index, state.shipments.size, state, Modifier.staggeredEnter(index + 3))
        }
        OrderSummary(state, Modifier.staggeredEnter(state.shipments.size + 3))
        ErrorBanner(state)
    }
}

@Composable
private fun DeliverToCard(state: CheckoutState, onChange: () -> Unit) {
    val a = state.address
    Section(Modifier.staggeredEnter(0)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            IconBadge(Icons.Rounded.LocationOn)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs / 2)) {
                Text(stringResource(Res.string.checkout_deliver_to, a.fullName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(a.line1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${a.city} ${a.postalCode}, ${a.country}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onChange) { Text(stringResource(Res.string.checkout_change), color = AmazgoneTheme.extended.cta) }
        }
    }
}

/** "Arrives Fri, 25 Sep · in 6 days" for one shipment with the chosen courier. */
@Composable
private fun shipmentArrival(shipment: Shipment, state: CheckoutState): String? {
    val window = CheckoutPlanner.courierArrival(listOf(shipment), state.destination, state.courier, state.now) ?: return null
    return stringResource(Res.string.checkout_arrives_on, stringResource(Res.string.checkout_courier_arrives, Formatters.weekdayDate(window.last), durationText(window.last - state.now)))
}

@Composable
private fun ShipmentCard(shipment: Shipment, index: Int, count: Int, state: CheckoutState, modifier: Modifier) {
    Section(modifier) {
        Text(stringResource(Res.string.checkout_shipment, index + 1, count), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (shipment.isDigital) stringResource(Res.string.checkout_instant) else shipmentArrival(shipment, state).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AmazgoneTheme.extended.success,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            StoreLogo(shipment.store, AmazgoneDimens.iconMd)
            Text(stringResource(Res.string.store_sold_by, shipment.store.name), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (shipment.store.isVerified) VerifiedIcon()
            if (shipment.isDigital) Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        shipment.lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small) {
                    ProductImage(line.product.thumbnailUrl, line.product.id, Modifier.size(AmazgoneDimens.iconLg + AmazgoneDimens.spaceSm).padding(AmazgoneDimens.spaceXs))
                }
                Column(Modifier.weight(1f)) {
                    Text(line.product.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(Res.string.checkout_qty, line.quantity), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CoinAmount(line.lineTotalCoins, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/** White rounded card used by every review section. */
@Composable
internal fun Section(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) { content() }
    }
}

@Composable
internal fun IconBadge(icon: ImageVector) {
    Surface(color = AmazgoneTheme.extended.cta.copy(alpha = BADGE_ALPHA), contentColor = AmazgoneTheme.extended.cta, shape = MaterialTheme.shapes.medium) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(AmazgoneDimens.spaceSm).size(AmazgoneDimens.iconMd))
    }
}

private const val BADGE_ALPHA = 0.14f
