package com.cikup.amazgone.orders.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_delivery
import amazgone.shared.generated.resources.checkout_discount
import amazgone.shared.generated.resources.checkout_free
import amazgone.shared.generated.resources.checkout_ship_to
import amazgone.shared.generated.resources.checkout_subtotal
import amazgone.shared.generated.resources.checkout_total
import amazgone.shared.generated.resources.order_added_to_cart
import amazgone.shared.generated.resources.order_detail_title
import amazgone.shared.generated.resources.order_paid_with
import amazgone.shared.generated.resources.order_payment
import amazgone.shared.generated.resources.order_received
import amazgone.shared.generated.resources.order_received_done
import amazgone.shared.generated.resources.order_received_hint
import amazgone.shared.generated.resources.order_rejected_reason
import amazgone.shared.generated.resources.order_xp_earned
import amazgone.shared.generated.resources.review_posted
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.shipments
import com.cikup.amazgone.orders.presentation.checkout.IconBadge
import com.cikup.amazgone.orders.presentation.checkout.Section
import com.cikup.amazgone.reviews.presentation.ReviewSheetModel
import com.cikup.amazgone.reviews.presentation.WriteReviewSheet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
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
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OrderDetailEffect.NavigateBack -> onBack()
                is OrderDetailEffect.NavigateToProduct -> onOpenProduct(effect.productId)
                OrderDetailEffect.ReceivedConfirmed -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(getString(Res.string.order_received_done))
                }
                OrderDetailEffect.ReviewPosted -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(getString(Res.string.review_posted))
                }
                OrderDetailEffect.AddedToCart -> snackbar.showSnackbar(getString(Res.string.order_added_to_cart))
            }
        }
    }
    OrderDetailScreen(state, viewModel::onIntent, snackbar)
}

@Composable
fun OrderDetailScreen(state: OrderDetailState, onIntent: (OrderDetailIntent) -> Unit, snackbar: SnackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(Res.string.order_detail_title), { onIntent(OrderDetailIntent.Back) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val order = state.order
        val stage = state.stage
        if (order != null && stage != null) OrderDetailContent(order, stage, state, PaddingValues(top = padding.calculateTopPadding()), onIntent)
    }
    state.reviewDraft?.let { draft ->
        WriteReviewSheet(
            model = ReviewSheetModel(draft.item.productId, draft.item.title, draft.item.thumbnailUrl, draft.rating, draft.comment, draft.problems, draft.isSubmitting),
            onRating = { onIntent(OrderDetailIntent.SetRating(it)) },
            onComment = { onIntent(OrderDetailIntent.SetComment(it)) },
            onSubmit = { onIntent(OrderDetailIntent.SubmitReview) },
            onDismiss = { onIntent(OrderDetailIntent.DismissReview) },
        )
    }
}

@Composable
private fun OrderDetailContent(order: Order, stage: OrderStage, state: OrderDetailState, padding: PaddingValues, onIntent: (OrderDetailIntent) -> Unit) {
    val shipments = order.shipments()
    LazyColumn(
        contentPadding = PaddingValues(
            start = AmazgoneDimens.spaceLg,
            end = AmazgoneDimens.spaceLg,
            top = padding.calculateTopPadding() + AmazgoneDimens.spaceSm,
            bottom = AmazgoneDimens.iconXl + AmazgoneDimens.spaceXl,
        ),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "tracker") { OrderTrackerCard(order, stage, Modifier.staggeredEnter(0)) }
        order.rejectionReason?.let { reason ->
            item(key = "rejected") {
                Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(Res.string.order_rejected_reason, reason), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg))
                }
            }
        }
        item(key = "received") {
            AnimatedVisibility(state.canConfirmReceived, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ReceivedPrompt { onIntent(OrderDetailIntent.ConfirmReceived) }
            }
        }
        itemsIndexed(shipments, key = { index, _ -> "shipment-$index" }) { index, shipment ->
            ShipmentSection(shipment, state::canReview, state.myReviews, onIntent, Modifier.staggeredEnter(index + 1))
        }
        item(key = "payment") { PaymentCard(order, Modifier.staggeredEnter(shipments.size + 1)) }
        item(key = "address") { AddressCard(order, Modifier.staggeredEnter(shipments.size + 2)) }
    }
}

@Composable
private fun ReceivedPrompt(onConfirm: () -> Unit) {
    Section {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            IconBadge(Icons.Rounded.CheckCircle)
            Text(stringResource(Res.string.order_received_hint), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta),
            modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget),
        ) { Text(stringResource(Res.string.order_received), style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun PaymentCard(order: Order, modifier: Modifier) {
    Section(modifier) {
        Text(stringResource(Res.string.order_payment), style = MaterialTheme.typography.titleMedium)
        Amount(Res.string.checkout_subtotal, order.subtotalCoins)
        if (order.discountCoins > 0) Amount(Res.string.checkout_discount, -order.discountCoins, AmazgoneTheme.extended.success)
        if (order.items.any { !it.digital }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.checkout_delivery), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (order.deliveryFeeCoins == 0L) {
                    Text(stringResource(Res.string.checkout_free), style = MaterialTheme.typography.bodyMedium, color = AmazgoneTheme.extended.success, fontWeight = FontWeight.Bold)
                } else {
                    CoinAmount(order.deliveryFeeCoins, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.checkout_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CoinAmount(order.totalCoins, style = MaterialTheme.typography.titleLarge.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(stringResource(Res.string.order_paid_with), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(stringResource(Res.string.order_xp_earned, Formatters.coins(order.xpEarned)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun Amount(label: StringResource, value: Long, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        if (value < 0) Text("−", style = MaterialTheme.typography.bodyMedium, color = color)
        CoinAmount(kotlin.math.abs(value), style = MaterialTheme.typography.bodyMedium.copy(color = color))
    }
}

@Composable
private fun AddressCard(order: Order, modifier: Modifier) {
    Section(modifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            IconBadge(Icons.Rounded.LocationOn)
            Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs / 2)) {
                Text(stringResource(Res.string.checkout_ship_to), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                with(order.address) {
                    Text(fullName, style = MaterialTheme.typography.bodyMedium)
                    Text(line1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$city $postalCode, $country", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
