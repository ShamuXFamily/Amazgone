package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_instant
import amazgone.shared.generated.resources.checkout_keep_shopping
import amazgone.shared.generated.resources.checkout_view_order
import amazgone.shared.generated.resources.order_placed_arriving_between
import amazgone.shared.generated.resources.order_placed_arriving_on
import amazgone.shared.generated.resources.order_placed_items
import amazgone.shared.generated.resources.order_placed_number
import amazgone.shared.generated.resources.order_placed_rewards
import amazgone.shared.generated.resources.order_placed_ship_to
import amazgone.shared.generated.resources.order_placed_spent
import amazgone.shared.generated.resources.order_placed_sync
import amazgone.shared.generated.resources.order_placed_title
import amazgone.shared.generated.resources.order_placed_xp
import amazgone.shared.generated.resources.store_sold_by
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.AnimatedCheckmark
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.arrival
import com.cikup.amazgone.orders.domain.model.shipments
import com.cikup.amazgone.orders.presentation.detail.ParcelTrackingCard
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Amazon-style "Order placed, thanks!": celebratory hero, when it arrives, what you bought and what you earned. */
@Composable
fun OrderPlacedScreen(order: Order, onIntent: (CheckoutIntent) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(order.id) { haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                Hero(order)
                Column(
                    Modifier.offset(y = -HERO_OVERLAP).padding(horizontal = AmazgoneDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                ) {
                    DeliveryCard(order, Modifier.staggeredEnter(1))
                    ParcelTrackingCard(order, order.createdAt, Modifier.staggeredEnter(2), showRows = false)
                    ItemsStrip(order, Modifier.staggeredEnter(2))
                    RewardsCard(order, Modifier.staggeredEnter(3))
                    Button(
                        onClick = { onIntent(CheckoutIntent.ViewOrder) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta),
                        modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm).staggeredEnter(4),
                    ) { Text(stringResource(Res.string.checkout_view_order), style = MaterialTheme.typography.titleSmall) }
                    OutlinedButton(
                        onClick = { onIntent(CheckoutIntent.KeepShopping) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget).staggeredEnter(5),
                    ) { Text(stringResource(Res.string.checkout_keep_shopping)) }
                }
            }
            ConfettiBurst(trigger = order.id, origin = androidx.compose.ui.geometry.Offset(0.5f, CONFETTI_Y))
        }
    }
}

@Composable
private fun Hero(order: Order) {
    val ext = AmazgoneTheme.extended
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, ext.cta, HERO_ACCENT))),
                RoundedCornerShape(bottomStart = AmazgoneDimens.spaceXl, bottomEnd = AmazgoneDimens.spaceXl),
            )
            .statusBarsPadding()
            .padding(start = AmazgoneDimens.spaceLg, end = AmazgoneDimens.spaceLg, top = AmazgoneDimens.spaceXl, bottom = AmazgoneDimens.spaceXl + HERO_OVERLAP),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
    ) {
        PulsingCheck(AmazgoneDimens.iconXl)
        Text(
            stringResource(Res.string.order_placed_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ext.onBrandNavy,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEnter(0),
        )
        Surface(color = ext.onBrandNavy.copy(alpha = CHIP_ALPHA), contentColor = ext.onBrandNavy, shape = CircleShape, modifier = Modifier.staggeredEnter(1)) {
            Text(
                stringResource(Res.string.order_placed_number, order.id.take(ORDER_NUMBER_LENGTH).uppercase()),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceXs),
            )
        }
    }
}

/** White disc with a self-drawing check and two rings that keep rippling outwards. */
@Composable
private fun PulsingCheck(size: Dp) {
    val ext = AmazgoneTheme.extended
    val reduceMotion = LocalReduceMotion.current
    val ripple by if (reduceMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "ripple").animateFloat(
            0f, 1f, infiniteRepeatable(tween(MotionTokens.DURATION_EXTRA_LONG_MS * 2, easing = LinearEasing), RepeatMode.Restart), label = "r",
        )
    }
    val pop = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) pop.animateTo(1f, MotionTokens.bouncy()) }
    Box(
        Modifier.size(size * RING_SPACE).drawBehind {
            if (!reduceMotion) {
                listOf(ripple, (ripple + 0.5f) % 1f).forEach { t ->
                    drawCircle(ext.onBrandNavy.copy(alpha = (1 - t) * RING_ALPHA), radius = size.toPx() / 2 * (1 + t * (RING_SPACE - 1)), style = Stroke(width = RING_STROKE))
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Surface(color = ext.onBrandNavy, shape = CircleShape, modifier = Modifier.size(size * pop.value)) {
            AnimatedCheckmark(ext.success, Modifier.padding(size / 5))
        }
    }
}

@Composable
private fun DeliveryCard(order: Order, modifier: Modifier) {
    Section(modifier) {
        order.arrival()?.let { window ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                IconBadge(Icons.Rounded.LocalShipping)
                Text(
                    if (window.first == window.last) {
                        stringResource(Res.string.order_placed_arriving_on, Formatters.weekdayDate(window.first))
                    } else {
                        stringResource(Res.string.order_placed_arriving_between, Formatters.weekdayDate(window.first), Formatters.weekdayDate(window.last))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmazgoneTheme.extended.success,
                )
            }
        }
        order.shipments().forEach { shipment ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Icon(if (shipment.digital) Icons.Rounded.QrCode2 else Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(
                    listOfNotNull(
                        shipment.storeName?.let { stringResource(Res.string.store_sold_by, it) },
                        if (shipment.digital) stringResource(Res.string.checkout_instant) else null,
                    ).joinToString(" · ").ifEmpty { pluralStringResource(Res.plurals.order_placed_items, shipment.items.size, shipment.items.size) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(
                stringResource(Res.string.order_placed_ship_to, order.address.fullName, order.address.city),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Overlapping thumbnails, like a stack of parcels. */
@Composable
private fun ItemsStrip(order: Order, modifier: Modifier) {
    Section(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                order.items.take(MAX_THUMBS).forEachIndexed { index, item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(AmazgoneDimens.spaceXs / 2, MaterialTheme.colorScheme.surfaceContainerLowest),
                        modifier = Modifier.offset(x = THUMB_STEP * index).staggeredEnter(index),
                    ) {
                        ProductImage(item.thumbnailUrl, item.productId, Modifier.size(AmazgoneDimens.iconLg + AmazgoneDimens.spaceSm).padding(AmazgoneDimens.spaceXs))
                    }
                }
            }
            Text(pluralStringResource(Res.plurals.order_placed_items, order.itemCount, order.itemCount), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun RewardsCard(order: Order, modifier: Modifier) {
    Section(modifier) {
        Text(stringResource(Res.string.order_placed_rewards), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            RewardTile(Icons.Rounded.AutoAwesome, stringResource(Res.string.order_placed_xp), "+", order.xpEarned, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            RewardTile(null, stringResource(Res.string.order_placed_spent), "−", order.totalCoins, AmazgoneTheme.extended.cta, Modifier.weight(1f))
        }
        if (order.status == OrderStatus.PENDING_SYNC) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.order_placed_sync), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Big number that counts up from zero when the page opens. */
@Composable
private fun RewardTile(icon: ImageVector?, label: String, sign: String, amount: Long, tint: Color, modifier: Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val shown = remember { Animatable(if (reduceMotion) amount.toFloat() else 0f) }
    LaunchedEffect(amount) { if (!reduceMotion) shown.animateTo(amount.toFloat(), tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedDecelerate)) }
    Surface(color = tint.copy(alpha = CHIP_ALPHA), shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$sign${Formatters.coins(shown.value.toLong())}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

private val HERO_OVERLAP = AmazgoneDimens.spaceXl
private val THUMB_STEP = AmazgoneDimens.iconLg * 0.75f
private const val MAX_THUMBS = 5
private const val ORDER_NUMBER_LENGTH = 8
private const val HERO_ACCENT = 0.4f
private const val CHIP_ALPHA = 0.14f
private const val RING_SPACE = 1.6f
private const val RING_ALPHA = 0.5f
private const val RING_STROKE = 3f
private const val CONFETTI_Y = 0.18f
