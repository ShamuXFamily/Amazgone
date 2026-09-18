package com.cikup.amazgone.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens

private const val TITLE_MAX_LINES = 2

/**
 * Grid/list tile for a product. [origin] namespaces the shared-element keys so the same product
 * can appear in several sections of one screen and still morph from the one the user tapped.
 */
@Composable
fun ProductCard(
    product: Product,
    origin: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    ElevatedCard(
        colors = appCardColors(),
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction),
    ) {
        Box {
            ProductImage(
                url = product.thumbnailUrl,
                productId = product.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .sharedElementOrNone(SharedKeys.image(origin, product.id))
                    .clip(MaterialTheme.shapes.medium),
            )
            DiscountBadge(
                percent = product.discountPercent,
                modifier = Modifier.align(Alignment.TopStart).padding(AmazgoneDimens.spaceSm),
            )
        }
        Column(
            modifier = Modifier.padding(AmazgoneDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = TITLE_MAX_LINES,
                minLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedKeys.title(origin, product.id)),
            )
            product.rating?.let { RatingStars(rating = it, count = product.ratingCount) }
            PriceBlock(product.priceCoins, product.originalPriceCoins)
        }
    }
}

/** Fixed-width variant for horizontal carousels. */
@Composable
fun CompactProductCard(product: Product, origin: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ProductCard(product, origin, onClick, modifier.width(AmazgoneDimens.productCardMinWidth))
}
