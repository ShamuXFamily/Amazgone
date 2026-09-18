package com.cikup.amazgone.catalog.presentation.home

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.flash_title
import amazgone.shared.generated.resources.flash_view_all
import amazgone.shared.generated.resources.home_all_products
import amazgone.shared.generated.resources.home_empty_body
import amazgone.shared.generated.resources.home_empty_title
import amazgone.shared.generated.resources.home_top_rated
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.presentation.components.FlashProductCard
import com.cikup.amazgone.core.designsystem.component.CompactProductCard
import com.cikup.amazgone.core.designsystem.component.CountdownBoxes
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.ProductCard
import com.cikup.amazgone.core.designsystem.component.ProductCardSkeleton
import com.cikup.amazgone.core.designsystem.component.SectionHeader
import com.cikup.amazgone.core.designsystem.component.StatusBarScrim
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val SKELETON_COUNT = 6
private const val ORIGIN_DEALS = "deals"
private const val ORIGIN_FLASH = "flash"
private const val ORIGIN_TOP = "top"
private const val ORIGIN_GRID = "grid"

@Composable
fun HomeRoute(
    onNavigate: (HomeDestination) -> Unit,
    onOpenProduct: (productId: String, origin: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.Navigate -> onNavigate(effect.destination)
                is HomeEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
            }
        }
    }
    HomeScreen(state, viewModel::onIntent)
}

@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(HomeIntent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
                contentPadding = PaddingValues(start = AmazgoneDimens.spaceLg, end = AmazgoneDimens.spaceLg, bottom = AmazgoneDimens.spaceXl),
                horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                modifier = Modifier.fillMaxSize(),
            ) {
                fullWidth("header") { HomeHeader(state.syncStatus, onIntent, Modifier.statusBarsPadding()) }
                fullWidth("quick") { QuickActions(onIntent) }
                when {
                    state.isLoading -> skeletons()
                    state.isEmpty -> fullWidth("empty") {
                        MessageState(Icons.Outlined.Storefront, stringResource(Res.string.home_empty_title), stringResource(Res.string.home_empty_body))
                    }
                    else -> feed(state, onIntent)
                }
            }
            StatusBarScrim(MaterialTheme.colorScheme.background)
        }
    }
}

private fun LazyGridScope.feed(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    if (state.deals.isNotEmpty()) {
        fullWidth("deals") { DealsCarousel(state.deals, onOpen = { onIntent(HomeIntent.OpenProduct(it, ORIGIN_DEALS)) }) }
    }
    state.flashSale?.takeIf { it.deals.isNotEmpty() }?.let { sale ->
        fullWidth("flash-title") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                Text(stringResource(Res.string.flash_title), style = MaterialTheme.typography.titleLarge)
                CountdownBoxes(
                    sale.remainingMillis(state.now),
                    container = AmazgoneTheme.extended.cta.copy(alpha = COUNTDOWN_TINT),
                    content = AmazgoneTheme.extended.cta,
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onIntent(HomeIntent.Open(HomeDestination.FLASH_SALE)) }) {
                        Text(stringResource(Res.string.flash_view_all), color = AmazgoneTheme.extended.cta)
                    }
                }
            }
        }
        fullWidth("flash") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                rowItemsIndexed(sale.deals, key = { _, d -> d.product.id }) { index, deal ->
                    FlashProductCard(
                        deal = deal,
                        origin = ORIGIN_FLASH,
                        saved = deal.product.id in state.savedIds,
                        onClick = { onIntent(HomeIntent.OpenProduct(deal.product.id, ORIGIN_FLASH)) },
                        onToggleSaved = { onIntent(HomeIntent.ToggleSaved(deal.product.id)) },
                        fixedWidth = true,
                        modifier = Modifier.staggeredEnter(index),
                    )
                }
            }
        }
    }
    fullWidth("categories") {
        CategoryChips(state.categories, state.selectedCategory, onSelect = { onIntent(HomeIntent.SelectCategory(it)) })
    }
    if (state.topRated.isNotEmpty() && state.selectedCategory == null) {
        fullWidth("top-title") { SectionHeader(stringResource(Res.string.home_top_rated), Modifier.padding(horizontal = AmazgoneDimens.none)) }
        fullWidth("top") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                rowItemsIndexed(state.topRated, key = { _, p -> p.id }) { _, product ->
                    CompactProductCard(product, ORIGIN_TOP, onClick = { onIntent(HomeIntent.OpenProduct(product.id, ORIGIN_TOP)) })
                }
            }
        }
    }
    fullWidth("grid-title") { SectionHeader(stringResource(Res.string.home_all_products)) }
    itemsIndexed(state.products, key = { _, p -> "grid-${p.id}" }) { index, product ->
        ProductCard(
            product = product,
            origin = ORIGIN_GRID,
            onClick = { onIntent(HomeIntent.OpenProduct(product.id, ORIGIN_GRID)) },
            modifier = Modifier.animateItem().staggeredEnter(index),
        )
    }
}

private const val COUNTDOWN_TINT = 0.14f

private fun LazyGridScope.skeletons() {
    items(SKELETON_COUNT, key = { "skeleton-$it" }) { ProductCardSkeleton() }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}
