package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_coupon_flat
import amazgone.shared.generated.resources.checkout_coupon_min
import amazgone.shared.generated.resources.checkout_coupon_percent
import amazgone.shared.generated.resources.checkout_coupons
import amazgone.shared.generated.resources.checkout_delivery
import amazgone.shared.generated.resources.checkout_discount
import amazgone.shared.generated.resources.checkout_free
import amazgone.shared.generated.resources.checkout_insufficient
import amazgone.shared.generated.resources.checkout_items_count
import amazgone.shared.generated.resources.checkout_left_after
import amazgone.shared.generated.resources.checkout_no_coupon
import amazgone.shared.generated.resources.checkout_payment_method
import amazgone.shared.generated.resources.checkout_total
import amazgone.shared.generated.resources.checkout_wallet_card
import amazgone.shared.generated.resources.checkout_xp_reward
import amazgone.shared.generated.resources.cart_summary_savings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.error.message
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.progress.domain.model.XpRules
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaymentSection(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Text(stringResource(Res.string.checkout_payment_method), style = MaterialTheme.typography.titleMedium)
        WalletCard(state.balance, state.balanceAfter)
        if (state.coupons.isNotEmpty()) {
            Text(stringResource(Res.string.checkout_coupons), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = AmazgoneDimens.spaceXs))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), contentPadding = PaddingValues(end = AmazgoneDimens.spaceLg)) {
                item { CouponTicket(null, state.selectedCouponCode == null) { onIntent(CheckoutIntent.SelectCoupon(null)) } }
                items(state.coupons, key = { it.code }) { coupon ->
                    CouponTicket(coupon, state.selectedCouponCode == coupon.code) { onIntent(CheckoutIntent.SelectCoupon(coupon.code)) }
                }
            }
        }
    }
}

/** The coin wallet drawn like a payment card: navy→orange gradient, balance, what's left after paying. */
@Composable
private fun WalletCard(balance: Long, after: Long) {
    val ext = AmazgoneTheme.extended
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, ext.cta, CARD_ACCENT))))
            .padding(AmazgoneDimens.spaceLg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.checkout_wallet_card), style = MaterialTheme.typography.labelLarge, color = ext.onBrandNavyVariant, modifier = Modifier.weight(1f))
                Box(Modifier.size(AmazgoneDimens.iconMd * 1.4f, AmazgoneDimens.iconMd).clip(MaterialTheme.shapes.extraSmall).background(ext.cta.copy(alpha = CHIP_ALPHA)))
            }
            CoinAmount(balance, style = MaterialTheme.typography.headlineMedium.copy(color = ext.onBrandNavy, fontWeight = FontWeight.Bold), iconSize = AmazgoneDimens.iconMd, animateChanges = true)
            val afterColor by animateColorAsState(if (after >= 0) ext.onBrandNavyVariant else MaterialTheme.colorScheme.errorContainer, label = "after")
            Text(stringResource(Res.string.checkout_left_after, Formatters.coins(after.coerceAtLeast(0))), style = MaterialTheme.typography.labelMedium, color = afterColor)
        }
    }
}

@Composable
private fun CouponTicket(coupon: Coupon?, selected: Boolean, onSelect: () -> Unit) {
    val cta = AmazgoneTheme.extended.cta
    val border by animateColorAsState(if (selected) cta else MaterialTheme.colorScheme.outlineVariant, label = "ticket")
    Surface(
        color = if (selected) cta.copy(alpha = CHIP_ALPHA) else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(AmazgoneDimens.spaceXs / 4, border),
        modifier = Modifier.clip(MaterialTheme.shapes.medium).selectable(selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            if (coupon != null) Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Column {
                Text(
                    when {
                        coupon == null -> stringResource(Res.string.checkout_no_coupon)
                        coupon.kind == CouponKind.PERCENT -> stringResource(Res.string.checkout_coupon_percent, coupon.value.toInt())
                        else -> stringResource(Res.string.checkout_coupon_flat, Formatters.coins(coupon.value))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) cta else MaterialTheme.colorScheme.onSurface,
                )
                coupon?.let {
                    Text(
                        if (it.minSubtotalCoins > 0) stringResource(Res.string.checkout_coupon_min, Formatters.coins(it.minSubtotalCoins)) else it.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Receipt: items at list price, savings, coupon, delivery, total and the XP this order earns. */
@Composable
fun OrderSummary(state: CheckoutState, modifier: Modifier = Modifier) {
    val summary = state.summary
    val success = AmazgoneTheme.extended.success
    Section(modifier) {
        SummaryLine(stringResource(Res.string.checkout_items_count, summary.itemCount), summary.subtotalCoins + summary.savingsCoins)
        AnimatedVisibility(summary.savingsCoins > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            SummaryLine(stringResource(Res.string.cart_summary_savings), -summary.savingsCoins, success)
        }
        AnimatedVisibility(summary.couponDiscountCoins > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            SummaryLine(stringResource(Res.string.checkout_discount), -summary.couponDiscountCoins, success)
        }
        if (state.needsDelivery) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.checkout_delivery), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (state.deliveryFeeCoins == 0L) {
                    Text(stringResource(Res.string.checkout_free), style = MaterialTheme.typography.bodyMedium, color = success, fontWeight = FontWeight.Bold)
                } else {
                    CoinAmount(state.deliveryFeeCoins, style = MaterialTheme.typography.bodyMedium, animateChanges = true)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.checkout_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            CoinAmount(state.totalCoins, style = MaterialTheme.typography.titleLarge.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold), animateChanges = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(stringResource(Res.string.checkout_xp_reward, Formatters.coins(XpRules.forOrder(summary.totalCoins))), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun SummaryLine(label: String, coins: Long, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        if (coins < 0) Text("−", style = MaterialTheme.typography.bodyMedium, color = color)
        CoinAmount(kotlin.math.abs(coins), style = MaterialTheme.typography.bodyMedium.copy(color = color), animateChanges = true)
    }
}

@Composable
internal fun ErrorBanner(state: CheckoutState) {
    AnimatedVisibility(state.error != null || !state.canAfford) {
        val error = state.error
        Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = MaterialTheme.shapes.medium) {
            Text(
                text = if (error != null) error.message() else stringResource(Res.string.checkout_insufficient, Formatters.coins(-state.balanceAfter)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceMd),
            )
        }
    }
}

private const val CARD_ACCENT = 0.45f
private const val CHIP_ALPHA = 0.18f
