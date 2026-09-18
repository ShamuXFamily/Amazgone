package com.cikup.amazgone.cart.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_checkout
import amazgone.shared.generated.resources.cart_play_for_coins
import amazgone.shared.generated.resources.cart_savings
import amazgone.shared.generated.resources.cart_summary_coupon_hint
import amazgone.shared.generated.resources.cart_summary_items
import amazgone.shared.generated.resources.cart_summary_savings
import amazgone.shared.generated.resources.cart_summary_title
import amazgone.shared.generated.resources.cart_total
import amazgone.shared.generated.resources.cart_wallet_after
import amazgone.shared.generated.resources.cart_wallet_enough
import amazgone.shared.generated.resources.cart_wallet_short
import amazgone.shared.generated.resources.cart_wallet_title
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

/** Navy card comparing the wallet with the cart total; the bar fills up to how much the wallet covers. */
@Composable
fun WalletCheckCard(balance: Long, total: Long, shortfall: Long, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val ext = AmazgoneTheme.extended
    val covered = if (total <= 0) 1f else (balance.toFloat() / total).coerceIn(0f, 1f)
    val fill by animateFloatAsState(covered, MotionTokens.gentle(), label = "walletFill")
    val barColor by animateColorAsState(if (shortfall == 0L) ext.success else MaterialTheme.colorScheme.tertiary, label = "walletBar")
    Surface(color = ext.brandNavy, contentColor = ext.onBrandNavy, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.cart_wallet_title), style = MaterialTheme.typography.labelLarge, color = ext.onBrandNavyVariant, modifier = Modifier.weight(1f))
                CoinAmount(balance, style = MaterialTheme.typography.titleLarge.copy(color = ext.onBrandNavy, fontWeight = FontWeight.Bold), iconSize = AmazgoneDimens.iconMd, animateChanges = true)
            }
            Box(
                Modifier.fillMaxWidth().height(AmazgoneDimens.spaceSm).clip(CircleShape).background(ext.onBrandNavy.copy(alpha = TRACK_ALPHA))
                    .drawBehind { drawRect(barColor, size = Size(size.width * fill, size.height)) },
            )
            AnimatedContent(shortfall == 0L, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "walletMsg") { enough ->
                if (enough) EnoughRow(balance - total, ext.success) else ShortRow(shortfall, onPlay)
            }
        }
    }
}

@Composable
private fun EnoughRow(left: Long, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(AmazgoneDimens.iconMd))
        Column {
            Text(stringResource(Res.string.cart_wallet_enough), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(Res.string.cart_wallet_after, Formatters.coins(left)), style = MaterialTheme.typography.labelMedium, color = AmazgoneTheme.extended.onBrandNavyVariant)
        }
    }
}

@Composable
private fun ShortRow(shortfall: Long, onPlay: () -> Unit) {
    val ext = AmazgoneTheme.extended
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Text(
            stringResource(Res.string.cart_wallet_short, Formatters.coins(shortfall)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta)) {
            Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs))
            Text(stringResource(Res.string.cart_play_for_coins), modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
        }
    }
}

/** Receipt-style breakdown at the end of the list. */
@Composable
fun OrderSummaryCard(summary: CartSummary, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Text(stringResource(Res.string.cart_summary_title), style = MaterialTheme.typography.titleMedium)
            SummaryRow(stringResource(Res.string.cart_summary_items, summary.itemCount), summary.subtotalCoins + summary.savingsCoins)
            AnimatedVisibility(summary.savingsCoins > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                SummaryRow(stringResource(Res.string.cart_summary_savings), summary.savingsCoins, negative = true, color = AmazgoneTheme.extended.success)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.cart_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                CoinAmount(summary.totalCoins, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), animateChanges = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.cart_summary_coupon_hint), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, coins: Long, negative: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        if (negative) Text("−", style = MaterialTheme.typography.bodyMedium, color = color)
        CoinAmount(coins, style = MaterialTheme.typography.bodyMedium.copy(color = color), animateChanges = true)
    }
}

/** Bottom bar: rolling total, savings chip and the checkout button side by side. */
@Composable
fun CheckoutBar(summary: CartSummary, onCheckout: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val ext = AmazgoneTheme.extended
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = AmazgoneDimens.spaceXl, topEnd = AmazgoneDimens.spaceXl),
        shadowElevation = AmazgoneDimens.spaceSm,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.cart_total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CoinAmount(summary.totalCoins, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), iconSize = AmazgoneDimens.iconMd, animateChanges = true)
                AnimatedVisibility(summary.savingsCoins > 0) {
                    Surface(color = ext.success.copy(alpha = TRACK_ALPHA), contentColor = ext.success, shape = CircleShape) {
                        Text(
                            "${stringResource(Res.string.cart_savings)} ${Formatters.coins(summary.savingsCoins)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2),
                        )
                    }
                }
            }
            Button(
                onClick = onCheckout,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta),
                modifier = Modifier.heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm).pressScale(interaction),
            ) {
                Text(stringResource(Res.string.cart_checkout), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

private const val TRACK_ALPHA = 0.15f
