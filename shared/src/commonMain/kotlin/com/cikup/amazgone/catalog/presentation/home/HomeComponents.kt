package com.cikup.amazgone.catalog.presentation.home

import com.cikup.amazgone.core.designsystem.component.appCardColors
import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.home_category_all
import amazgone.shared.generated.resources.home_search_hint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.lerp
import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.component.DiscountBadge
import com.cikup.amazgone.core.designsystem.component.PriceBlock
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.component.SharedKeys
import com.cikup.amazgone.core.designsystem.component.sharedElementOrNone
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.OnScrimColor
import com.cikup.amazgone.core.presentation.format.Formatters
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue

private const val AUTO_ADVANCE_MS = 4_000L
private const val CAROUSEL_HEIGHT_FRACTION = 0.9f
private const val PAGE_MIN_SCALE = 0.88f
private const val PAGE_MIN_ALPHA = 0.5f
private const val PARALLAX_SHIFT = 0.35f
private const val SCRIM_ALPHA = 0.75f
private const val CHIP_TINT = 0.14f

@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = AmazgoneDimens.spaceXs / 2,
        modifier = modifier.fillMaxWidth().pressScale(interaction),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceLg, vertical = AmazgoneDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.home_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Hero deals: auto-advances (paused while dragging), neighbours shrink/fade, art moves with parallax. */
@Composable
fun DealsCarousel(deals: List<Product>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { deals.size }
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(pager, dragged, deals.size) {
        if (dragged || reduceMotion || deals.size < 2) return@LaunchedEffect
        while (true) {
            delay(AUTO_ADVANCE_MS)
            pager.animateScrollToPage((pager.currentPage + 1) % deals.size)
        }
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceSm),
            pageSpacing = AmazgoneDimens.spaceMd,
            snapPosition = SnapPosition.Center,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            DealPage(deals[page], pageOffset = { pager.offsetFor(page) }, onClick = { onOpen(deals[page].id) })
        }
        PagerDots(count = deals.size, current = pager.currentPage)
    }
}

private fun PagerState.offsetFor(page: Int): Float = (currentPage - page) + currentPageOffsetFraction

@Composable
private fun DealPage(product: Product, pageOffset: () -> Float, onClick: () -> Unit) {
    ElevatedCard(
        colors = appCardColors(),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val distance = pageOffset().absoluteValue.coerceIn(0f, 1f)
                val scale = lerp(1f, PAGE_MIN_SCALE, distance)
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, PAGE_MIN_ALPHA, distance)
            },
    ) {
        Box(Modifier.fillMaxWidth().height(AmazgoneDimens.productCardMinWidth / CAROUSEL_HEIGHT_FRACTION)) {
            ProductImage(
                url = product.imageUrls.firstOrNull() ?: product.thumbnailUrl,
                productId = product.id,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElementOrNone(SharedKeys.image("deals", product.id))
                    .graphicsLayer { translationX = pageOffset() * size.width * PARALLAX_SHIFT },
            )
            DealCaption(product, Modifier.align(Alignment.BottomStart))
            DiscountBadge(product.discountPercent, Modifier.align(Alignment.TopEnd).padding(AmazgoneDimens.spaceMd))
        }
    }
}

@Composable
private fun DealCaption(product: Product, modifier: Modifier = Modifier) {
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(scrim.copy(alpha = 0f), scrim)))
            .padding(AmazgoneDimens.spaceLg),
    ) {
        Text(
            product.title,
            style = MaterialTheme.typography.titleMedium,
            color = OnScrimColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CompositionLocalProvider(LocalContentColor provides OnScrimColor) {
            PriceBlock(product.priceCoins, product.originalPriceCoins)
        }
    }
}

/** Department chips; the selected one springs its check mark in and resizes smoothly. */
@Composable
fun CategoryChips(
    categories: List<Category>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        item(key = "all") { CategoryChip(stringResource(Res.string.home_category_all), selected == null) { onSelect(null) } }
        items(categories, key = { it.slug }) { category ->
            CategoryChip(Formatters.categoryLabel(category.slug), selected == category.slug) { onSelect(category.slug) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AmazgoneTheme.extended.cta.copy(alpha = CHIP_TINT),
            selectedLabelColor = AmazgoneTheme.extended.cta,
            selectedLeadingIconColor = AmazgoneTheme.extended.cta,
        ),
        leadingIcon = if (isSelected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.height(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        modifier = Modifier.animateContentSize(MotionTokens.bouncy()),
    )
}

/** Indicator dots: the active one stretches into a pill with a spring. */
@Composable
fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) AmazgoneDimens.spaceXl else AmazgoneDimens.spaceSm, MotionTokens.bouncy(), label = "dotWidth")
            val color by animateColorAsState(
                if (selected) AmazgoneTheme.extended.cta else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(Modifier.height(AmazgoneDimens.spaceSm).width(width).clip(MaterialTheme.shapes.small).background(color))
        }
    }
}
