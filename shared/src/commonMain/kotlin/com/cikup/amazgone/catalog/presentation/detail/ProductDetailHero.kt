package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_added_check
import amazgone.shared.generated.resources.detail_add_to_cart
import amazgone.shared.generated.resources.detail_ratings
import amazgone.shared.generated.resources.detail_see_less
import amazgone.shared.generated.resources.detail_see_more
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.presentation.home.PagerDots
import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.DiscountBadge
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.RatingStars
import com.cikup.amazgone.core.designsystem.component.RollingNumber
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.sharedBoundsOrNone
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.wishlist.presentation.HeartButton
import org.jetbrains.compose.resources.stringResource

private val STAGE_CORNER = 32.dp
private const val PARALLAX = 0.5f
private const val GAME_ASPECT = 460f / 215f
private const val COLLAPSED_LINES = 3
private const val EXPANDED_ROTATION = 90f
private const val CARD_RISE_PX = 120f

/** White rounded stage: pager (first page morphs from the tapped card), parallax, animated dots. */
@Composable
fun ImageStage(product: Product, origin: String, scrollOffset: () -> Int, modifier: Modifier = Modifier) {
    val images = product.imageUrls.ifEmpty { listOf(product.thumbnailUrl) }
    val pager = rememberPagerState { images.size }
    val aspect = if (product.source == CatalogSourceId.CHEAP_SHARK) GAME_ASPECT else 1f
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(bottomStart = STAGE_CORNER, bottomEnd = STAGE_CORNER))
            .statusBarsPadding()
            .padding(bottom = AmazgoneDimens.spaceMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .padding(horizontal = AmazgoneDimens.spaceXl, vertical = AmazgoneDimens.spaceXl)
                .graphicsLayer { translationY = scrollOffset() * PARALLAX }
                .sharedElementOrNone(SharedKeys.image(origin, product.id)),
        ) { page -> ProductImage(images[page], product.id, Modifier.fillMaxSize()) }
        if (images.size > 1) PagerDots(images.size, pager.currentPage)
    }
}

/** "4.5 ★" chip that pops in with a spring. */
@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) scale.animateTo(1f, MotionTokens.bouncy()) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = AmazgoneDimens.spaceXs / 2,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
            Text(Formatters.rating(rating), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Rounded.Star, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
        }
    }
}

/** Rounded info card that rises into place; heart sits on its edge; description expands smoothly. */
@Composable
fun InfoCard(product: Product, state: ProductDetailState, onIntent: (ProductDetailIntent) -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val rise = remember { Animatable(if (reduceMotion) 0f else CARD_RISE_PX) }
    LaunchedEffect(Unit) { if (!reduceMotion) rise.animateTo(0f, MotionTokens.gentle()) }
    Box(Modifier.padding(horizontal = AmazgoneDimens.spaceLg).graphicsLayer { translationY = rise.value; alpha = 1f - rise.value / CARD_RISE_PX }) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = AmazgoneDimens.iconXl / 2).sharedBoundsOrNone(SharedKeys.title(state.origin, product.id)),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                    CoinAmount(
                        product.priceCoins,
                        style = MaterialTheme.typography.headlineSmall.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold),
                        iconSize = AmazgoneDimens.iconMd,
                    )
                    product.originalPriceCoins?.let {
                        Text(Formatters.coins(it), style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DiscountBadge(product.discountPercent)
                }
                product.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                        RatingStars(rating, animate = true)
                        product.ratingCount?.let {
                            Text(stringResource(Res.string.detail_ratings, Formatters.compactCount(it.toLong())), style = MaterialTheme.typography.labelMedium, color = AmazgoneTheme.extended.cta)
                        }
                    }
                }
                ExpandableDescription(product)
                StockLabelPublic(product)
            }
        }
        HeartButton(state.isSaved, { onIntent(ProductDetailIntent.ToggleWishlist) }, Modifier.align(Alignment.TopEnd).padding(AmazgoneDimens.spaceMd))
    }
}

@Composable
private fun ExpandableDescription(product: Product) {
    val text = descriptionOf(product) ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) EXPANDED_ROTATION else 0f, MotionTokens.bouncy(), label = "arrow")
    Column(Modifier.animateContentSize(MotionTokens.gentle())) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) Res.string.detail_see_less else Res.string.detail_see_more), color = AmazgoneTheme.extended.cta)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AmazgoneTheme.extended.cta, modifier = Modifier.rotate(arrow))
        }
    }
}

/** Quantity stepper + big orange CTA that morphs into a check after adding. */
@Composable
fun BuyBar(product: Product, state: ProductDetailState, onIntent: (ProductDetailIntent) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val colors = AmazgoneTheme.extended
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = AmazgoneDimens.spaceSm) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = AmazgoneDimens.spaceLg, vertical = AmazgoneDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        ) {
            val stepper = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            FilledTonalIconButton(onClick = { onIntent(ProductDetailIntent.ChangeQuantity(state.quantity - 1)) }, enabled = state.quantity > 1, colors = stepper) {
                Icon(Icons.Rounded.Remove, contentDescription = null)
            }
            RollingNumber(state.quantity, style = MaterialTheme.typography.titleMedium)
            FilledTonalIconButton(
                onClick = { onIntent(ProductDetailIntent.ChangeQuantity(state.quantity + 1)) },
                enabled = state.quantity < CartCalculator.MAX_QUANTITY_PER_ITEM,
                colors = stepper,
            ) { Icon(Icons.Rounded.Add, contentDescription = null) }
            Button(
                onClick = { onIntent(ProductDetailIntent.AddToCart) },
                enabled = product.isInStock && !state.isAdding,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(containerColor = colors.cta, contentColor = colors.onCta),
                modifier = Modifier.weight(1f).heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm).pressScale(interaction),
            ) {
                AnimatedContent(
                    targetState = state.justAdded,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn() + scaleIn(MotionTokens.bouncy())).togetherWith(slideOutVertically { -it } + fadeOut())
                    },
                    label = "addCta",
                ) { added ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                        Icon(if (added) Icons.Rounded.Check else Icons.Rounded.AddShoppingCart, contentDescription = null)
                        Text(stringResource(if (added) Res.string.detail_added_check else Res.string.detail_add_to_cart), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
