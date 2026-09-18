package com.cikup.amazgone.orders.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_discount
import amazgone.shared.generated.resources.checkout_ship_to
import amazgone.shared.generated.resources.checkout_subtotal
import amazgone.shared.generated.resources.checkout_total
import amazgone.shared.generated.resources.order_detail_title
import amazgone.shared.generated.resources.order_rejected_reason
import amazgone.shared.generated.resources.order_timeline_placed
import amazgone.shared.generated.resources.order_timeline_shipping
import amazgone.shared.generated.resources.order_timeline_synced
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.presentation.OrderStatusChip
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun OrderDetailRoute(
    orderId: String,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    viewModel: OrderDetailViewModel = koinViewModel(key = orderId) { parametersOf(orderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OrderDetailEffect.NavigateBack -> onBack()
                is OrderDetailEffect.NavigateToProduct -> onOpenProduct(effect.productId)
            }
        }
    }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.order_detail_title), { viewModel.onIntent(OrderDetailIntent.Back) }) }) { padding ->
        state.order?.let { OrderDetailContent(it, PaddingValues(top = padding.calculateTopPadding()), viewModel::onIntent) }
    }
}

@Composable
private fun OrderDetailContent(order: Order, padding: PaddingValues, onIntent: (OrderDetailIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = AmazgoneDimens.spaceLg,
            end = AmazgoneDimens.spaceLg,
            top = padding.calculateTopPadding() + AmazgoneDimens.spaceSm,
            bottom = AmazgoneDimens.spaceXl,
        ),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { OrderStatusChip(order.status, Modifier.staggeredEnter(0)) }
        item { Timeline(order.status, Modifier.staggeredEnter(1)) }
        order.rejectionReason?.let { reason ->
            item { Text(stringResource(Res.string.order_rejected_reason, reason), color = MaterialTheme.colorScheme.error) }
        }
        item {
            ElevatedCard(Modifier.staggeredEnter(2)) {
                order.items.forEach { item ->
                    ListItem(
                        leadingContent = {
                            ProductImage(item.thumbnailUrl, item.productId, Modifier.size(AmazgoneDimens.iconLg).clip(MaterialTheme.shapes.small))
                        },
                        headlineContent = { Text(item.title, maxLines = 2) },
                        supportingContent = { Text("× ${item.quantity}") },
                        trailingContent = { CoinAmount(item.unitPriceCoins * item.quantity, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.padding(vertical = AmazgoneDimens.spaceXs),
                    )
                }
            }
        }
        item { Totals(order, Modifier.staggeredEnter(3)) }
        item {
            ElevatedCard(Modifier.fillMaxWidth().staggeredEnter(4)) {
                Column(Modifier.padding(AmazgoneDimens.spaceLg)) {
                    Text(stringResource(Res.string.checkout_ship_to), style = MaterialTheme.typography.titleMedium)
                    with(order.address) {
                        Text(fullName)
                        Text(line1)
                        Text("$city $postalCode, $country")
                    }
                }
            }
        }
    }
}

/** Steps light up in sequence; the connector fills with a spring when the server confirms. */
@Composable
private fun Timeline(status: OrderStatus, modifier: Modifier = Modifier) {
    val reached = when (status) {
        OrderStatus.PENDING_SYNC -> 1
        OrderStatus.CONFIRMED -> 3
        OrderStatus.REJECTED -> 1
    }
    val steps = listOf(Res.string.order_timeline_placed, Res.string.order_timeline_synced, Res.string.order_timeline_shipping)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        steps.forEachIndexed { index, label -> TimelineRow(label, index < reached, isLast = index == steps.lastIndex, nextDone = index + 1 < reached) }
    }
}

@Composable
private fun TimelineRow(label: StringResource, done: Boolean, isLast: Boolean, nextDone: Boolean) {
    val dot by animateColorAsState(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dot")
    val fill by animateFloatAsState(if (nextDone) 1f else 0f, MotionTokens.gentle(), label = "connector")
    val track = MaterialTheme.colorScheme.outlineVariant
    val active = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(AmazgoneDimens.spaceMd).clip(CircleShape).background(dot))
            if (!isLast) {
                Box(Modifier.width(AmazgoneDimens.spaceXs / 2).height(AmazgoneDimens.spaceXl).background(track)) {
                    Box(Modifier.fillMaxSize().graphicsLayer { scaleY = fill; transformOrigin = TransformOrigin(0.5f, 0f) }.background(active))
                }
            }
        }
        Text(
            stringResource(label),
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Totals(order: Order, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Amount(Res.string.checkout_subtotal, order.subtotalCoins)
            if (order.discountCoins > 0) Amount(Res.string.checkout_discount, -order.discountCoins)
            Amount(Res.string.checkout_total, order.totalCoins)
        }
    }
}

@Composable
private fun Amount(label: StringResource, value: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label))
        CoinAmount(value, style = MaterialTheme.typography.bodyLarge)
    }
}
