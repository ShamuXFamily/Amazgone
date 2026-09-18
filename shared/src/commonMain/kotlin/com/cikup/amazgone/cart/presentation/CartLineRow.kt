package com.cikup.amazgone.cart.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_decrease
import amazgone.shared.generated.resources.cart_delete
import amazgone.shared.generated.resources.cart_each
import amazgone.shared.generated.resources.cart_increase
import amazgone.shared.generated.resources.cart_remove
import amazgone.shared.generated.resources.cart_save_for_later
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.DiscountBadge
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RollingNumber
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.appCardColors
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

/** Swipe end-to-start to delete; the red background and bin icon grow as the swipe passes the threshold. */
@Composable
fun CartLineRow(line: CartLine, onIntent: (CartIntent) -> Unit, modifier: Modifier = Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onIntent(CartIntent.Remove(line.product.id)) },
        backgroundContent = { SwipeBackground(armed = dismiss.targetValue == SwipeToDismissBoxValue.EndToStart) },
        modifier = modifier,
    ) {
        ElevatedCard(onClick = { onIntent(CartIntent.OpenProduct(line.product.id)) }, colors = appCardColors(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    ImageTile(line)
                    LineInfo(line, Modifier.weight(1f))
                }
                LineActions(line, onIntent)
            }
        }
    }
}

@Composable
private fun SwipeBackground(armed: Boolean) {
    val color by animateColorAsState(if (armed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer, label = "swipeBg")
    val iconScale by animateFloatAsState(if (armed) ARMED_ICON_SCALE else 1f, MotionTokens.bouncy(), label = "swipeIcon")
    Box(
        Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(color).padding(AmazgoneDimens.spaceXl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Rounded.Delete, stringResource(Res.string.cart_remove), tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.scale(iconScale))
    }
}

@Composable
private fun ImageTile(line: CartLine) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        ProductImage(
            line.product.thumbnailUrl,
            line.product.id,
            Modifier.size(AmazgoneDimens.iconXl).padding(AmazgoneDimens.spaceXs)
                .sharedElementOrNone(SharedKeys.image(CART_ORIGIN, line.product.id))
                .clip(MaterialTheme.shapes.small),
        )
    }
}

@Composable
private fun LineInfo(line: CartLine, modifier: Modifier) {
    val product = line.product
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            CoinAmount(line.lineTotalCoins, style = MaterialTheme.typography.titleMedium.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold), animateChanges = true)
            product.originalPriceCoins?.let {
                Text(
                    Formatters.coins(it * line.quantity),
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DiscountBadge(product.discountPercent)
        }
        AnimatedVisibility(line.quantity > 1, enter = fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(Res.string.cart_each, Formatters.coins(product.priceCoins)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LineActions(line: CartLine, onIntent: (CartIntent) -> Unit) {
    val id = line.product.id
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QuantityPill(line.quantity, onChange = { onIntent(CartIntent.ChangeQuantity(id, it)) })
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            ActionButton(Icons.Outlined.BookmarkBorder, stringResource(Res.string.cart_save_for_later)) { onIntent(CartIntent.SaveForLater(id)) }
            ActionButton(Icons.Outlined.DeleteOutline, stringResource(Res.string.cart_delete)) { onIntent(CartIntent.Remove(id)) }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
    }
}

/** Compact "− 1 +" pill; the minus becomes a bin at quantity 1. */
@Composable
private fun QuantityPill(quantity: Int, onChange: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val change = { next: Int ->
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onChange(next)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { change(quantity - 1) }) {
                Icon(if (quantity == 1) Icons.Rounded.Delete else Icons.Rounded.Remove, stringResource(Res.string.cart_decrease), modifier = Modifier.size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs))
            }
            RollingNumber(quantity, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { change(quantity + 1) }, enabled = quantity < CartCalculator.MAX_QUANTITY_PER_ITEM) {
                Icon(Icons.Rounded.Add, stringResource(Res.string.cart_increase), modifier = Modifier.size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs))
            }
        }
    }
}

private const val ARMED_ICON_SCALE = 1.3f
