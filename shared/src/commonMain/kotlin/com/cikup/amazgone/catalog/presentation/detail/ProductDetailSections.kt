package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_brand
import amazgone.shared.generated.resources.detail_game_description
import amazgone.shared.generated.resources.detail_in_stock
import amazgone.shared.generated.resources.detail_key_facts
import amazgone.shared.generated.resources.detail_low_stock
import amazgone.shared.generated.resources.detail_metacritic
import amazgone.shared.generated.resources.detail_min_order
import amazgone.shared.generated.resources.detail_no_reviews
import amazgone.shared.generated.resources.detail_out_of_five
import amazgone.shared.generated.resources.detail_out_of_stock
import amazgone.shared.generated.resources.detail_preorder
import amazgone.shared.generated.resources.detail_reviews_count
import amazgone.shared.generated.resources.detail_sku
import amazgone.shared.generated.resources.detail_steam_rating
import amazgone.shared.generated.resources.detail_verified
import amazgone.shared.generated.resources.review_yours
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.Review
import com.cikup.amazgone.core.designsystem.component.RatingStars
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val LOW_STOCK_THRESHOLD = 10
private const val STAR_LEVELS = 5
private const val TRACK_ALPHA = 0.15f

/** Coloured availability pill: green in stock, gold low stock, red unavailable. */
@Composable
fun StockPill(product: Product, preorder: Boolean) {
    val stock = product.stock ?: return
    val releaseDay = product.details.releaseDateMillis
    val (text, color) = when {
        preorder && releaseDay != null ->
            stringResource(Res.string.detail_preorder, Formatters.shortDate(releaseDay, utc = true)) to AmazgoneTheme.extended.cta
        stock <= 0 -> stringResource(Res.string.detail_out_of_stock) to MaterialTheme.colorScheme.error
        stock < LOW_STOCK_THRESHOLD -> stringResource(Res.string.detail_low_stock, stock) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(Res.string.detail_in_stock) to AmazgoneTheme.extended.success
    }
    Surface(color = color.copy(alpha = TRACK_ALPHA), contentColor = color, shape = CircleShape) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(AmazgoneDimens.spaceSm).clip(CircleShape).background(color))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
        }
    }
}

/** Delivery / warranty / returns as small icon badges (only the ones the product has). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrustBadges(product: Product, modifier: Modifier = Modifier) {
    val d = product.details
    val badges = listOfNotNull(
        d.shipping?.let { Icons.Outlined.LocalShipping to it },
        d.warranty?.let { Icons.Outlined.VerifiedUser to it },
        d.returnPolicy?.let { Icons.Outlined.AssignmentReturn to it },
    )
    if (badges.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        badges.forEachIndexed { index, (icon, text) ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.staggeredEnter(index)) {
                Row(Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
                    Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
                }
            }
        }
    }
}

/** Description text, or a generic line for game deals (which have none). */
@Composable
fun descriptionOf(product: Product): String? = product.description.ifBlank {
    if (product.source == CatalogSourceId.CHEAP_SHARK) stringResource(Res.string.detail_game_description) else null
}

private data class Fact(val icon: ImageVector, val label: StringResource, val value: String)

/** Compact two-column grid of the product's facts; tiles cascade in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyFacts(product: Product, modifier: Modifier = Modifier) {
    val d = product.details
    val facts = listOfNotNull(
        product.brand?.let { Fact(Icons.Outlined.Sell, Res.string.detail_brand, it) },
        d.ratingLabel?.let { Fact(Icons.Outlined.ThumbUp, Res.string.detail_steam_rating, it) },
        d.metacriticScore?.let { Fact(Icons.Outlined.Verified, Res.string.detail_metacritic, "$it / 100") },
        d.minimumOrderQuantity?.let { Fact(Icons.Outlined.Inventory2, Res.string.detail_min_order, it.toString()) },
        d.sku?.let { Fact(Icons.Outlined.QrCode2, Res.string.detail_sku, it) },
    )
    if (facts.isEmpty()) return
    Column(modifier.padding(horizontal = AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Text(stringResource(Res.string.detail_key_facts), style = MaterialTheme.typography.titleLarge)
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            facts.forEachIndexed { index, fact ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).staggeredEnter(index),
                ) {
                    Row(Modifier.padding(AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                        Surface(color = AmazgoneTheme.extended.cta.copy(alpha = TRACK_ALPHA), shape = MaterialTheme.shapes.small) {
                            Icon(fact.icon, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.padding(AmazgoneDimens.spaceXs).size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs))
                        }
                        Column {
                            Text(stringResource(fact.label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(fact.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** Big average + stars + count, and 5★..1★ distribution bars that fill one after another. */
@Composable
fun RatingSummary(rating: Double?, reviews: List<Review>, modifier: Modifier = Modifier) {
    if (reviews.isEmpty() && rating == null) return
    val average = rating ?: reviews.map { it.rating }.average()
    val counts = (STAR_LEVELS downTo 1).map { star -> star to reviews.count { it.rating == star } }
    val max = counts.maxOf { it.second }.coerceAtLeast(1)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = modifier.padding(horizontal = AmazgoneDimens.spaceLg).fillMaxWidth()) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXl), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Formatters.rating(average), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.detail_out_of_five), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RatingStars(average, animate = true)
                Text(stringResource(Res.string.detail_reviews_count, reviews.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                counts.forEachIndexed { index, (star, count) -> DistributionBar(star, count.toFloat() / max, index) }
            }
        }
    }
}

@Composable
private fun DistributionBar(star: Int, fraction: Float, index: Int) {
    val reduceMotion = LocalReduceMotion.current
    val fill = remember { Animatable(if (reduceMotion) fraction else 0f) }
    LaunchedEffect(fraction) {
        if (reduceMotion) return@LaunchedEffect
        delay(MotionTokens.staggerDelayMillis(index).toLong())
        fill.animateTo(fraction, MotionTokens.gentle())
    }
    val color = AmazgoneTheme.extended.cta
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Text(star.toString(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(AmazgoneDimens.spaceSm))
        Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(AmazgoneDimens.iconSm * 0.8f))
        Box(
            Modifier
                .weight(1f)
                .height(AmazgoneDimens.spaceSm)
                .clip(CircleShape)
                .background(color.copy(alpha = TRACK_ALPHA))
                .drawBehind { drawRect(color, size = Size(size.width * fill.value, size.height)) },
        )
    }
}

@Composable
fun ReviewCard(review: Review, modifier: Modifier = Modifier, mine: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = if (mine) BorderStroke(AmazgoneDimens.spaceXs / 2, AmazgoneTheme.extended.cta) else null,
        modifier = modifier.fillMaxWidth().padding(horizontal = AmazgoneDimens.spaceLg),
    ) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Avatar(review.reviewerName)
                Column(Modifier.weight(1f)) {
                    Text(if (mine) stringResource(Res.string.review_yours) else review.reviewerName, style = MaterialTheme.typography.titleSmall, color = if (mine) AmazgoneTheme.extended.cta else MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = AmazgoneTheme.extended.success, modifier = Modifier.size(AmazgoneDimens.iconSm * 0.8f))
                        Text(stringResource(Res.string.detail_verified), style = MaterialTheme.typography.labelSmall, color = AmazgoneTheme.extended.success, modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
                    }
                }
                if (review.dateMillis > 0) {
                    Text(Formatters.shortDate(review.dateMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RatingStars(review.rating.toDouble())
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Avatar(name: String) {
    val tints = AmazgoneTheme.extended.tileTints
    val tint: Color = tints[name.sumOf { it.code } % tints.size]
    Box(Modifier.size(AmazgoneDimens.iconLg * 0.75f).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = AmazgoneTheme.extended.brandNavy)
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
