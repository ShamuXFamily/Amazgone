package com.cikup.amazgone.catalog.presentation.home

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.badge_new
import amazgone.shared.generated.resources.badge_preorder
import amazgone.shared.generated.resources.home_new_arrivals
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.appCardColors
import com.cikup.amazgone.core.designsystem.component.sharedBoundsOrNone
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

/** Header + carousel of curated launches; each card flags NEW or PRE-ORDER (release date still ahead). */
@Composable
fun NewArrivalsRow(products: List<Product>, now: Long, origin: String, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AmazgoneTheme.extended.cta)
            Text(stringResource(Res.string.home_new_arrivals), style = MaterialTheme.typography.titleLarge)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            itemsIndexed(products, key = { _, p -> p.id }) { index, product ->
                val preorder = (product.details.releaseDateMillis ?: 0) > now && now > 0
                NewArrivalCard(product, preorder, origin, { onOpen(product.id) }, Modifier.staggeredEnter(index))
            }
        }
    }
}

@Composable
private fun NewArrivalCard(product: Product, preorder: Boolean, origin: String, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    ElevatedCard(
        onClick = onClick,
        colors = appCardColors(),
        interactionSource = interaction,
        modifier = modifier.width(AmazgoneDimens.productCardMinWidth).pressScale(interaction),
    ) {
        Box {
            Surface(color = AmazgoneTheme.extended.imageStage, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(AmazgoneDimens.spaceSm)) {
                ProductImage(
                    url = product.thumbnailUrl,
                    productId = product.id,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(AmazgoneDimens.spaceSm)
                        .sharedElementOrNone(SharedKeys.image(origin, product.id)),
                )
            }
            LaunchBadge(preorder, Modifier.align(Alignment.TopStart).padding(AmazgoneDimens.spaceMd))
        }
        Column(Modifier.padding(start = AmazgoneDimens.spaceMd, end = AmazgoneDimens.spaceMd, bottom = AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            product.brand?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                product.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedKeys.title(origin, product.id)),
            )
            CoinAmount(product.priceCoins, style = MaterialTheme.typography.titleMedium.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun LaunchBadge(preorder: Boolean, modifier: Modifier) {
    val ext = AmazgoneTheme.extended
    Surface(
        color = if (preorder) ext.brandNavy else ext.cta,
        contentColor = if (preorder) ext.onBrandNavy else ext.onCta,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2), verticalAlignment = Alignment.CenterVertically) {
            if (!preorder) Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm * 0.75f))
            Text(
                stringResource(if (preorder) Res.string.badge_preorder else Res.string.badge_new),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = AmazgoneDimens.spaceXs / 2),
            )
        }
    }
}
