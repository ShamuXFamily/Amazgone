package com.cikup.amazgone.wishlist.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_add_to_cart
import amazgone.shared.generated.resources.detail_low_stock
import amazgone.shared.generated.resources.detail_out_of_stock
import amazgone.shared.generated.resources.detail_preorder
import amazgone.shared.generated.resources.wishlist_in_cart
import amazgone.shared.generated.resources.wishlist_remove
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.DiscountBadge
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RatingStars
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.appCardColors
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.stores.presentation.StoreLogo
import com.cikup.amazgone.stores.presentation.VerifiedIcon
import org.jetbrains.compose.resources.stringResource

/** One saved product; swipe left (or tap the bin) to remove, with undo offered by the screen. */
@Composable
fun WishlistRow(product: Product, inCart: Boolean, now: Long, onIntent: (WishlistIntent) -> Unit, modifier: Modifier = Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onIntent(WishlistIntent.Remove(product.id)) },
        backgroundContent = { RemoveBackground(armed = dismiss.targetValue == SwipeToDismissBoxValue.EndToStart) },
        modifier = modifier,
    ) {
        ElevatedCard(onClick = { onIntent(WishlistIntent.OpenProduct(product.id)) }, colors = appCardColors(), shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                    ProductImage(
                        product.thumbnailUrl,
                        product.id,
                        Modifier.size(IMAGE_SIZE).padding(AmazgoneDimens.spaceXs).sharedElementOrNone(SharedKeys.image(WISHLIST_ORIGIN, product.id)),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                        StoreLogo(product.store, AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs)
                        Text(product.store.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (product.store.isVerified) VerifiedIcon(AmazgoneDimens.iconSm * 0.8f)
                    }
                    Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    product.rating?.let { RatingStars(it, count = product.ratingCount) }
                    PriceLine(product)
                    StatusLine(product, now)
                    Actions(product, inCart, onIntent)
                }
            }
        }
    }
}

@Composable
private fun PriceLine(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        CoinAmount(product.priceCoins, style = MaterialTheme.typography.titleMedium.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold))
        product.originalPriceCoins?.let {
            Text(Formatters.coins(it), style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DiscountBadge(product.discountPercent)
    }
}

/** Pre-order, sold out or "Only N left" — nothing when there's plenty. */
@Composable
private fun StatusLine(product: Product, now: Long) {
    val release = product.details.releaseDateMillis
    val stock = product.stock
    val (text, color) = when {
        now > 0 && product.isPreorderAt(now) && release != null ->
            stringResource(Res.string.detail_preorder, Formatters.shortDate(release, utc = true)) to AmazgoneTheme.extended.cta
        !product.isInStock -> stringResource(Res.string.detail_out_of_stock) to MaterialTheme.colorScheme.error
        stock != null && stock < LOW_STOCK -> stringResource(Res.string.detail_low_stock, stock) to MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
}

@Composable
private fun Actions(product: Product, inCart: Boolean, onIntent: (WishlistIntent) -> Unit) {
    val ext = AmazgoneTheme.extended
    Row(Modifier.fillMaxWidth().padding(top = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            inCart,
            transitionSpec = { (scaleIn(MotionTokens.bouncy()) + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "inCart",
        ) { added ->
            if (added) {
                FilledTonalButton(onClick = { onIntent(WishlistIntent.AddToCart(product.id)) }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = ext.success.copy(alpha = TINT), contentColor = ext.success)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                    Text(stringResource(Res.string.wishlist_in_cart), modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
                }
            } else {
                Button(
                    onClick = { onIntent(WishlistIntent.AddToCart(product.id)) },
                    enabled = product.isInStock,
                    colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta),
                ) {
                    Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                    Text(stringResource(Res.string.detail_add_to_cart), modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
                }
            }
        }
        IconButton(onClick = { onIntent(WishlistIntent.Remove(product.id)) }) {
            Icon(Icons.Outlined.DeleteOutline, stringResource(Res.string.wishlist_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RemoveBackground(armed: Boolean) {
    val color by animateColorAsState(if (armed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer, label = "removeBg")
    val scale by animateFloatAsState(if (armed) ARMED_SCALE else 1f, MotionTokens.bouncy(), label = "removeIcon")
    Box(Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(color).padding(AmazgoneDimens.spaceXl), contentAlignment = Alignment.CenterEnd) {
        Icon(Icons.Rounded.Delete, stringResource(Res.string.wishlist_remove), tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.scale(scale))
    }
}

private val IMAGE_SIZE = AmazgoneDimens.iconXl
private const val LOW_STOCK = 10
private const val TINT = 0.15f
private const val ARMED_SCALE = 1.3f
