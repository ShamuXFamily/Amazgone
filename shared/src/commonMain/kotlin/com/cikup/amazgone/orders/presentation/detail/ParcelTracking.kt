package com.cikup.amazgone.orders.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_courier_arrives
import amazgone.shared.generated.resources.tracking_arrived
import amazgone.shared.generated.resources.tracking_packing
import amazgone.shared.generated.resources.tracking_percent
import amazgone.shared.generated.resources.tracking_route
import amazgone.shared.generated.resources.tracking_title
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
import com.cikup.amazgone.delivery.presentation.courierName
import com.cikup.amazgone.delivery.presentation.map.DeliveryMap
import com.cikup.amazgone.delivery.presentation.map.MapRoute
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.Parcel
import com.cikup.amazgone.orders.domain.model.parcels
import com.cikup.amazgone.orders.domain.model.point
import com.cikup.amazgone.orders.presentation.checkout.Section
import org.jetbrains.compose.resources.stringResource

/** Live map + one progress row per parcel. Couriers only move once the server has confirmed the order. */
@Composable
fun ParcelTrackingCard(order: Order, now: Long, modifier: Modifier = Modifier, showRows: Boolean = true) {
    val courier = order.courier ?: return
    val parcels = order.parcels()
    if (parcels.isEmpty()) return
    val moving = order.status == OrderStatus.CONFIRMED
    val progress = parcels.map { if (moving) CourierPlan.progress(it.departsAt, it.arrivalAt, now) else 0.0 }
    Section(modifier) {
        Text(stringResource(Res.string.tracking_title), style = MaterialTheme.typography.titleMedium)
        DeliveryMap(
            routes = parcels.mapIndexed { i, p -> MapRoute(p.origin.point, progress[i], courier.emoji) },
            home = order.address.point(),
            modifier = Modifier.fillMaxWidth().height(MAP_HEIGHT),
        )
        if (showRows) {
            parcels.forEachIndexed { index, parcel ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ParcelRow(courier, parcel, progress[index], order, now)
            }
        }
    }
}

@Composable
private fun ParcelRow(courier: Courier, parcel: Parcel, progress: Double, order: Order, now: Long) {
    val animated by animateFloatAsState(progress.toFloat(), MotionTokens.gentle(), label = "parcel")
    val ext = AmazgoneTheme.extended
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Text(courier.emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(courierName(courier), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.tracking_route, parcel.origin.city, order.address.city.ifBlank { order.address.country }, Formatters.coins(parcel.distanceKm.toLong())),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(Res.string.tracking_percent, (progress * PERCENT).toInt()), style = MaterialTheme.typography.labelLarge, color = ext.cta, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { animated },
            color = ext.cta,
            trackColor = ext.cta.copy(alpha = TRACK_ALPHA),
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            when {
                progress >= 1.0 -> stringResource(Res.string.tracking_arrived, Formatters.weekdayDate(parcel.arrivalAt))
                progress <= 0.0 && now < parcel.departsAt -> stringResource(Res.string.tracking_packing, parcel.origin.city)
                else -> stringResource(Res.string.checkout_courier_arrives, Formatters.weekdayDate(parcel.arrivalAt), durationText(parcel.arrivalAt - now))
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (progress >= 1.0) ext.success else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val MAP_HEIGHT = AmazgoneDimens.iconXl * 2.3f
private const val PERCENT = 100
private const val TRACK_ALPHA = 0.15f
