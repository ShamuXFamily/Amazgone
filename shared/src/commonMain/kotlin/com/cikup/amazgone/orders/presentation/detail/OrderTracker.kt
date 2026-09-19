package com.cikup.amazgone.orders.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.order_placed_number
import amazgone.shared.generated.resources.order_placed_date
import amazgone.shared.generated.resources.order_stage_confirmed
import amazgone.shared.generated.resources.order_stage_delivered
import amazgone.shared.generated.resources.order_stage_placed
import amazgone.shared.generated.resources.order_stage_rejected
import amazgone.shared.generated.resources.order_stage_shipped
import amazgone.shared.generated.resources.order_step_confirmed
import amazgone.shared.generated.resources.order_step_delivered
import amazgone.shared.generated.resources.order_step_placed
import amazgone.shared.generated.resources.order_step_shipped
import amazgone.shared.generated.resources.order_placed_arriving_between
import amazgone.shared.generated.resources.order_placed_arriving_on
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.arrival
import org.jetbrains.compose.resources.stringResource

/** Navy header card: where the order is, when it arrives, and a 4-step tracker that fills with a spring. */
@Composable
fun OrderTrackerCard(order: Order, stage: OrderStage, modifier: Modifier = Modifier) {
    val ext = AmazgoneTheme.extended
    val rejected = stage == OrderStage.REJECTED
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, if (rejected) MaterialTheme.colorScheme.error else ext.cta, ACCENT))))
            .padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.order_placed_number, order.id.take(ORDER_NUMBER_LENGTH).uppercase()),
                style = MaterialTheme.typography.labelLarge,
                color = ext.onBrandNavyVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.order_placed_date, Formatters.shortDate(order.createdAt)), style = MaterialTheme.typography.labelMedium, color = ext.onBrandNavyVariant)
        }
        AnimatedContent(stage, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "stage") { current ->
            Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Text(stringResource(stageTitle(current)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ext.onBrandNavy)
                arrivalLine(order, current)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ext.onBrandNavyVariant) }
            }
        }
        if (!rejected) Tracker(stage)
    }
}

@Composable
private fun arrivalLine(order: Order, stage: OrderStage): String? {
    val window = order.arrival() ?: return null
    if (stage == OrderStage.DELIVERED) return order.deliveredAt?.let { Formatters.weekdayDate(it) }
    return if (window.first == window.last) {
        stringResource(Res.string.order_placed_arriving_on, Formatters.weekdayDate(window.first))
    } else {
        stringResource(Res.string.order_placed_arriving_between, Formatters.weekdayDate(window.first), Formatters.weekdayDate(window.last))
    }
}

private data class Step(val stage: OrderStage, val icon: ImageVector, val label: org.jetbrains.compose.resources.StringResource)

private val STEPS = listOf(
    Step(OrderStage.PLACED, Icons.Rounded.Receipt, Res.string.order_step_placed),
    Step(OrderStage.CONFIRMED, Icons.Rounded.Inventory2, Res.string.order_step_confirmed),
    Step(OrderStage.SHIPPED, Icons.Rounded.LocalShipping, Res.string.order_step_shipped),
    Step(OrderStage.DELIVERED, Icons.Rounded.Home, Res.string.order_step_delivered),
)

@Composable
private fun Tracker(stage: OrderStage) {
    val ext = AmazgoneTheme.extended
    val reached = STEPS.indexOfFirst { it.stage == stage }.coerceAtLeast(0)
    val progress by animateFloatAsState(reached / (STEPS.size - 1f), MotionTokens.gentle(), label = "track")
    Box {
        // Track behind the dots: from the first dot's centre to the last one's.
        Box(
            Modifier.fillMaxWidth().padding(horizontal = DOT_SIZE / 2, vertical = DOT_SIZE / 2 - TRACK_HEIGHT / 2).height(TRACK_HEIGHT)
                .clip(CircleShape).background(ext.onBrandNavy.copy(alpha = TRACK_ALPHA))
                .drawBehind { drawRect(ext.cta, size = Size(size.width * progress, size.height)) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            STEPS.forEachIndexed { index, step -> TrackerDot(step, done = index <= reached, current = index == reached) }
        }
    }
}

@Composable
private fun TrackerDot(step: Step, done: Boolean, current: Boolean) {
    val ext = AmazgoneTheme.extended
    val color by animateColorAsState(if (done) ext.cta else ext.brandNavy, label = "dot")
    val scale by animateFloatAsState(if (current) CURRENT_SCALE else 1f, MotionTokens.bouncy(), label = "dotScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Surface(
            color = color,
            contentColor = if (done) ext.onCta else ext.onBrandNavyVariant,
            shape = CircleShape,
            border = if (done) null else androidx.compose.foundation.BorderStroke(TRACK_HEIGHT / 2, ext.onBrandNavy.copy(alpha = TRACK_ALPHA * 2)),
            modifier = Modifier.size(DOT_SIZE * scale),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(step.icon, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm)) }
        }
        Text(
            stringResource(step.label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (done) ext.onBrandNavy else ext.onBrandNavyVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun stageTitle(stage: OrderStage) = when (stage) {
    OrderStage.PLACED -> Res.string.order_stage_placed
    OrderStage.CONFIRMED -> Res.string.order_stage_confirmed
    OrderStage.SHIPPED -> Res.string.order_stage_shipped
    OrderStage.DELIVERED -> Res.string.order_stage_delivered
    OrderStage.REJECTED -> Res.string.order_stage_rejected
}

private val DOT_SIZE = AmazgoneDimens.iconLg * 0.75f
private val TRACK_HEIGHT = AmazgoneDimens.spaceXs
private const val TRACK_ALPHA = 0.18f
private const val CURRENT_SCALE = 1.12f
private const val ACCENT = 0.35f
private const val ORDER_NUMBER_LENGTH = 8
