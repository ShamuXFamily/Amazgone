package com.cikup.amazgone.catalog.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.FlashDeal
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.SoldBadge
import com.cikup.amazgone.core.designsystem.component.sharedBoundsOrNone
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.wishlist.presentation.HeartButton

/** Flash-sale tile: image on a soft plate, heart, animated "% sold" bar, struck + orange price. */
@Composable
fun FlashProductCard(
    deal: FlashDeal,
    origin: String,
    saved: Boolean,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier,
    fixedWidth: Boolean = false,
) {
    val product = deal.product
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.then(if (fixedWidth) Modifier.width(AmazgoneDimens.productCardMinWidth) else Modifier).pressScale(interaction),
    ) {
        Box(Modifier.padding(AmazgoneDimens.spaceSm)) {
            ProductImage(
                url = product.thumbnailUrl,
                productId = product.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .sharedElementOrNone(SharedKeys.image(origin, product.id)),
            )
            HeartButton(saved = saved, onToggle = onToggleSaved, modifier = Modifier.align(Alignment.TopEnd))
            SoldBadge(deal.soldFraction, Modifier.align(Alignment.BottomStart).padding(AmazgoneDimens.spaceSm).fillMaxWidth(SOLD_WIDTH))
        }
        Column(Modifier.padding(horizontal = AmazgoneDimens.spaceMd).padding(bottom = AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Text(
                product.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedKeys.title(origin, product.id)),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                product.originalPriceCoins?.let {
                    Text(
                        Formatters.coins(it),
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CoinAmount(
                    product.priceCoins,
                    style = MaterialTheme.typography.titleMedium.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

private const val SOLD_WIDTH = 0.7f
