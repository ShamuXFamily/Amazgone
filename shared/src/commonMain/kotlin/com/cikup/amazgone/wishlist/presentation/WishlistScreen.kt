package com.cikup.amazgone.wishlist.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_browse
import amazgone.shared.generated.resources.cart_item_removed
import amazgone.shared.generated.resources.cart_undo
import amazgone.shared.generated.resources.sort_discount
import amazgone.shared.generated.resources.sort_price_high
import amazgone.shared.generated.resources.sort_price_low
import amazgone.shared.generated.resources.wishlist_add_all
import amazgone.shared.generated.resources.wishlist_added
import amazgone.shared.generated.resources.wishlist_all_in_cart
import amazgone.shared.generated.resources.wishlist_empty_body
import amazgone.shared.generated.resources.wishlist_empty_title
import amazgone.shared.generated.resources.wishlist_filter_all
import amazgone.shared.generated.resources.wishlist_filter_sale
import amazgone.shared.generated.resources.wishlist_filter_stock
import amazgone.shared.generated.resources.wishlist_items
import amazgone.shared.generated.resources.wishlist_no_match
import amazgone.shared.generated.resources.wishlist_on_sale
import amazgone.shared.generated.resources.wishlist_sort_recent
import amazgone.shared.generated.resources.wishlist_title
import amazgone.shared.generated.resources.wishlist_worth
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WishlistRoute(
    onBack: () -> Unit,
    onOpenProduct: (String, String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WishlistEffect.NavigateBack -> onBack()
                WishlistEffect.NavigateHome -> onBrowse()
                is WishlistEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
                is WishlistEffect.AddedToCart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(getPluralString(Res.plurals.wishlist_added, effect.count, effect.count))
                }
                is WishlistEffect.ShowUndo -> {
                    val result = snackbar.showSnackbar(getString(Res.string.cart_item_removed, effect.title), actionLabel = getString(Res.string.cart_undo))
                    if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(WishlistIntent.Undo(effect.productId))
                }
            }
        }
    }
    WishlistScreen(state, viewModel::onIntent, snackbar)
}

@Composable
fun WishlistScreen(state: WishlistState, onIntent: (WishlistIntent) -> Unit, snackbar: SnackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(Res.string.wishlist_title), { onIntent(WishlistIntent.Back) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.isLoading && state.products.isEmpty()) {
            MessageState(
                Icons.Outlined.FavoriteBorder,
                stringResource(Res.string.wishlist_empty_title),
                stringResource(Res.string.wishlist_empty_body),
                Modifier.padding(padding).fillMaxSize(),
                actionLabel = stringResource(Res.string.cart_browse),
                onAction = { onIntent(WishlistIntent.Browse) },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AmazgoneDimens.spaceLg,
                    end = AmazgoneDimens.spaceLg,
                    top = padding.calculateTopPadding() + AmazgoneDimens.spaceSm,
                    bottom = AmazgoneDimens.iconXl + AmazgoneDimens.spaceXl,
                ),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "summary") { SummaryCard(state, onIntent, Modifier.staggeredEnter(0)) }
                item(key = "filters") { Filters(state, onIntent) }
                if (state.visible.isEmpty()) {
                    item(key = "no-match") {
                        Text(stringResource(Res.string.wishlist_no_match), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(AmazgoneDimens.spaceLg))
                    }
                }
                itemsIndexed(state.visible, key = { _, p -> p.id }) { index, product ->
                    WishlistRow(product, product.id in state.inCart, state.now, onIntent, Modifier.animateItem().staggeredEnter(index + 1))
                }
            }
        }
    }
}

/** Navy card: how many items, what they're worth, what's on sale, and one tap to put them all in the cart. */
@Composable
private fun SummaryCard(state: WishlistState, onIntent: (WishlistIntent) -> Unit, modifier: Modifier) {
    val ext = AmazgoneTheme.extended
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, ext.cta, ACCENT))))
            .padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            Surface(color = ext.cta, contentColor = ext.onCta, shape = CircleShape) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.padding(AmazgoneDimens.spaceSm).size(AmazgoneDimens.iconMd))
            }
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(Res.plurals.wishlist_items, state.products.size, state.products.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ext.onBrandNavy)
                Text(stringResource(Res.string.wishlist_worth, Formatters.coins(state.totalCoins)), style = MaterialTheme.typography.labelLarge, color = ext.onBrandNavyVariant)
            }
        }
        if (state.onSaleCount > 0) {
            Surface(color = ext.onBrandNavy.copy(alpha = CHIP_ALPHA), contentColor = ext.onBrandNavy, shape = CircleShape) {
                Row(Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                    Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = ext.cta, modifier = Modifier.size(AmazgoneDimens.iconSm))
                    Text(stringResource(Res.string.wishlist_on_sale, state.onSaleCount, Formatters.coins(state.savingsCoins)), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        AnimatedContent(state.addable.size, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "addAll") { count ->
            if (count > 0) {
                Button(
                    onClick = { onIntent(WishlistIntent.AddAllToCart) },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta),
                    modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget),
                ) {
                    Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm + AmazgoneDimens.spaceXs))
                    Text(stringResource(Res.string.wishlist_add_all, count), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = AmazgoneDimens.spaceSm))
                }
            } else {
                Text(stringResource(Res.string.wishlist_all_in_cart), style = MaterialTheme.typography.labelLarge, color = ext.onBrandNavyVariant)
            }
        }
    }
}

@Composable
private fun Filters(state: WishlistState, onIntent: (WishlistIntent) -> Unit) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = AmazgoneTheme.extended.cta.copy(alpha = CHIP_ALPHA),
        selectedLabelColor = AmazgoneTheme.extended.cta,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            items(WishlistFilter.entries) { filter ->
                FilterChip(state.filter == filter, { onIntent(WishlistIntent.SetFilter(filter)) }, { Text(stringResource(filter.label())) }, colors = colors)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            items(WishlistSort.entries) { sort ->
                FilterChip(state.sort == sort, { onIntent(WishlistIntent.SetSort(sort)) }, { Text(stringResource(sort.label())) }, colors = colors)
            }
        }
    }
}

private fun WishlistFilter.label(): StringResource = when (this) {
    WishlistFilter.ALL -> Res.string.wishlist_filter_all
    WishlistFilter.ON_SALE -> Res.string.wishlist_filter_sale
    WishlistFilter.IN_STOCK -> Res.string.wishlist_filter_stock
}

private fun WishlistSort.label(): StringResource = when (this) {
    WishlistSort.RECENT -> Res.string.wishlist_sort_recent
    WishlistSort.PRICE_LOW -> Res.string.sort_price_low
    WishlistSort.PRICE_HIGH -> Res.string.sort_price_high
    WishlistSort.DISCOUNT -> Res.string.sort_discount
}

private const val ACCENT = 0.35f
private const val CHIP_ALPHA = 0.15f
