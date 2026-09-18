package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_about
import amazgone.shared.generated.resources.detail_brand
import amazgone.shared.generated.resources.detail_game_description
import amazgone.shared.generated.resources.detail_in_stock
import amazgone.shared.generated.resources.detail_low_stock
import amazgone.shared.generated.resources.detail_metacritic
import amazgone.shared.generated.resources.detail_min_order
import amazgone.shared.generated.resources.detail_no_reviews
import amazgone.shared.generated.resources.detail_out_of_stock
import amazgone.shared.generated.resources.detail_returns
import amazgone.shared.generated.resources.detail_shipping
import amazgone.shared.generated.resources.detail_sku
import amazgone.shared.generated.resources.detail_specs
import amazgone.shared.generated.resources.detail_steam_rating
import amazgone.shared.generated.resources.detail_warranty
import amazgone.shared.generated.resources.detail_you_save
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.Review
import com.cikup.amazgone.core.designsystem.component.DiscountBadge
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RatingStars
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.sharedBoundsOrNone
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

private const val LOW_STOCK_THRESHOLD = 10

@Composable
fun StockLabelPublic(product: Product) {
    val stock = product.stock
    val (text, color) = when {
        stock == null -> return
        stock <= 0 -> stringResource(Res.string.detail_out_of_stock) to MaterialTheme.colorScheme.error
        stock < LOW_STOCK_THRESHOLD -> stringResource(Res.string.detail_low_stock, stock) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(Res.string.detail_in_stock) to MaterialTheme.colorScheme.primary
    }
    Text(text, color = color, style = MaterialTheme.typography.titleSmall)
}

/** Description text, or a generic line for game deals (which have none). */
@Composable
fun descriptionOf(product: Product): String? = product.description.ifBlank {
    if (product.source == CatalogSourceId.CHEAP_SHARK) stringResource(Res.string.detail_game_description) else null
}

@Composable
fun SpecsSection(product: Product, modifier: Modifier = Modifier) {
    val d = product.details
    val rows = listOfNotNull(
        product.brand?.let { Res.string.detail_brand to it },
        d.ratingLabel?.let { Res.string.detail_steam_rating to it },
        d.metacriticScore?.let { Res.string.detail_metacritic to it.toString() },
        d.warranty?.let { Res.string.detail_warranty to it },
        d.shipping?.let { Res.string.detail_shipping to it },
        d.returnPolicy?.let { Res.string.detail_returns to it },
        d.minimumOrderQuantity?.let { Res.string.detail_min_order to it.toString() },
        d.sku?.let { Res.string.detail_sku to it },
    )
    if (rows.isEmpty()) return
    ElevatedCard(
        modifier.padding(horizontal = AmazgoneDimens.spaceLg),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Text(
            stringResource(Res.string.detail_specs),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(AmazgoneDimens.spaceLg),
        )
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider()
            ListItem(
                headlineContent = { Text(value) },
                overlineContent = { Text(stringResource(label)) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            )
        }
    }
}

@Composable
fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth().padding(horizontal = AmazgoneDimens.spaceLg)) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Avatar(review.reviewerName)
                Text(review.reviewerName, style = MaterialTheme.typography.titleSmall)
            }
            RatingStars(review.rating.toDouble())
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        Modifier.size(AmazgoneDimens.iconMd + AmazgoneDimens.spaceSm).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun NoReviews() {
    Text(
        stringResource(Res.string.detail_no_reviews),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceLg),
    )
}
