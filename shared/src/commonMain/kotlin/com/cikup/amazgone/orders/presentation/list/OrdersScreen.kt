package com.cikup.amazgone.orders.presentation.list

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.order_items_count
import amazgone.shared.generated.resources.order_placed_on
import amazgone.shared.generated.resources.orders_empty_body
import amazgone.shared.generated.resources.orders_empty_title
import amazgone.shared.generated.resources.orders_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.presentation.OrderStatusChip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val SHORT_ID = 8
private const val PREVIEW_IMAGES = 3

@Composable
fun OrdersRoute(onBack: () -> Unit, onOpenOrder: (String) -> Unit, viewModel: OrdersViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OrdersEffect.NavigateBack -> onBack()
                is OrdersEffect.NavigateToOrder -> onOpenOrder(effect.orderId)
            }
        }
    }
    OrdersScreen(state, viewModel::onIntent)
}

@Composable
fun OrdersScreen(state: OrdersState, onIntent: (OrdersIntent) -> Unit) {
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.orders_title), { onIntent(OrdersIntent.Back) }) }) { padding ->
        if (!state.isLoading && state.orders.isEmpty()) {
            MessageState(
                Icons.Outlined.Receipt,
                stringResource(Res.string.orders_empty_title),
                stringResource(Res.string.orders_empty_body),
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                itemsIndexed(state.orders, key = { _, o -> o.id }) { index, order ->
                    OrderCard(order, { onIntent(OrdersIntent.OpenOrder(order.id)) }, Modifier.animateItem().staggeredEnter(index))
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.order_placed_on, order.id.take(SHORT_ID).uppercase()), style = MaterialTheme.typography.titleSmall)
                OrderStatusChip(order.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                order.items.take(PREVIEW_IMAGES).forEach { item ->
                    ProductImage(item.thumbnailUrl, item.productId, Modifier.size(AmazgoneDimens.iconLg).clip(MaterialTheme.shapes.small))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.order_items_count, order.itemCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                CoinAmount(order.totalCoins)
            }
        }
    }
}
