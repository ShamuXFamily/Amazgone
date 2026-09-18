package com.cikup.amazgone.cart.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_decrease
import amazgone.shared.generated.resources.cart_increase
import amazgone.shared.generated.resources.cart_remove
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.core.designsystem.component.PriceBlock
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RollingNumber
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import org.jetbrains.compose.resources.stringResource

/** Swipe end-to-start to delete; background tints red as the swipe progresses. */
@Composable
fun CartLineRow(line: CartLine, onIntent: (CartIntent) -> Unit, modifier: Modifier = Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onIntent(CartIntent.Remove(line.product.id)) },
        backgroundContent = {
            val color by animateColorAsState(
                if (dismiss.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                label = "swipeBg",
            )
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(AmazgoneDimens.spaceXl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, stringResource(Res.string.cart_remove), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        modifier = modifier,
    ) {
        ElevatedCard(
            onClick = { onIntent(CartIntent.OpenProduct(line.product.id)) },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Row(Modifier.padding(AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                ProductImage(
                    line.product.thumbnailUrl,
                    line.product.id,
                    Modifier.size(AmazgoneDimens.iconXl)
                        .sharedElementOrNone(SharedKeys.image(CART_ORIGIN, line.product.id))
                        .clip(MaterialTheme.shapes.small),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                    Text(line.product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    PriceBlock(line.product.priceCoins, line.product.originalPriceCoins)
                    QuantityStepper(line.quantity, onChange = { onIntent(CartIntent.ChangeQuantity(line.product.id, it)) })
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        FilledTonalIconButton(onClick = { onChange(quantity - 1) }) {
            Icon(if (quantity == 1) Icons.Rounded.Delete else Icons.Rounded.Remove, stringResource(Res.string.cart_decrease))
        }
        RollingNumber(quantity, style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = { onChange(quantity + 1) }, enabled = quantity < CartCalculator.MAX_QUANTITY_PER_ITEM) {
            Icon(Icons.Rounded.Add, stringResource(Res.string.cart_increase))
        }
    }
}
