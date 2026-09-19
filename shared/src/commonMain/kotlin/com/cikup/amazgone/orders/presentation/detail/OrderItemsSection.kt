package com.cikup.amazgone.orders.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_instant
import amazgone.shared.generated.resources.checkout_qty
import amazgone.shared.generated.resources.order_buy_again
import amazgone.shared.generated.resources.order_edit_review
import amazgone.shared.generated.resources.order_review_pending
import amazgone.shared.generated.resources.order_write_review
import amazgone.shared.generated.resources.review_yours
import amazgone.shared.generated.resources.store_sold_by
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RatingStars
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.PlacedShipment
import com.cikup.amazgone.orders.presentation.checkout.Section
import com.cikup.amazgone.reviews.domain.model.ProductReview
import com.cikup.amazgone.stores.domain.model.StoreDirectory
import com.cikup.amazgone.stores.presentation.StoreLogo
import com.cikup.amazgone.stores.presentation.VerifiedIcon
import org.jetbrains.compose.resources.stringResource

/** One card per seller; delivered items get a review call-to-action (or show the review already written). */
@Composable
fun ShipmentSection(
    shipment: PlacedShipment,
    canReview: (OrderItem) -> Boolean,
    myReviews: Map<String, ProductReview>,
    onIntent: (OrderDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier) {
        val name = shipment.storeName
        // Orders placed before store ids were recorded only know the seller's name: show it without guessing a logo.
        val store = shipment.items.first().storeId?.let { id -> name?.let { StoreDirectory.fromIdAndName(id, it) } }
        if (name != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                if (store != null) StoreLogo(store, AmazgoneDimens.iconMd)
                Text(stringResource(Res.string.store_sold_by, name), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                if (store?.isVerified == true) VerifiedIcon()
            }
            if (shipment.digital) Text(stringResource(Res.string.checkout_instant), style = MaterialTheme.typography.labelMedium, color = AmazgoneTheme.extended.success)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        shipment.items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ItemRow(item, canReview(item), myReviews[item.productId], onIntent)
        }
    }
}

@Composable
private fun ItemRow(item: OrderItem, canReview: Boolean, review: ProductReview?, onIntent: (OrderDetailIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Row(
            Modifier.fillMaxWidth().clickable { onIntent(OrderDetailIntent.OpenProduct(item.productId)) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small) {
                ProductImage(item.thumbnailUrl, item.productId, Modifier.size(AmazgoneDimens.iconLg + AmazgoneDimens.spaceMd).padding(AmazgoneDimens.spaceXs))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(Res.string.checkout_qty, item.quantity), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CoinAmount(item.unitPriceCoins * item.quantity, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            AnimatedContent(
                targetState = review,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "reviewState",
            ) { current ->
                when {
                    current != null -> MyReviewChip(current) { onIntent(OrderDetailIntent.WriteReview(item.productId)) }
                    canReview -> FilledTonalButton(
                        onClick = { onIntent(OrderDetailIntent.WriteReview(item.productId)) },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmazgoneTheme.extended.cta.copy(alpha = TINT), contentColor = AmazgoneTheme.extended.cta),
                    ) {
                        Icon(Icons.Rounded.RateReview, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                        Text(stringResource(Res.string.order_write_review), modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
                    }
                    else -> Row {}
                }
            }
            TextButton(onClick = { onIntent(OrderDetailIntent.BuyAgain(item.productId)) }) {
                Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.order_buy_again), modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
            }
        }
    }
}

/** "Your review ★★★★★ · Edit" (with a syncing hint while it's only on this device). */
@Composable
private fun MyReviewChip(review: ProductReview, onEdit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.clickable(onClick = onEdit)) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Column {
                Text(stringResource(if (review.isPending) Res.string.order_review_pending else Res.string.review_yours), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RatingStars(review.rating.toDouble())
            }
            Text(stringResource(Res.string.order_edit_review), style = MaterialTheme.typography.labelLarge, color = AmazgoneTheme.extended.cta)
        }
    }
}

private const val TINT = 0.14f
