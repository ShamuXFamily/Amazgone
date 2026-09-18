package com.cikup.amazgone.catalog.presentation.flash

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.flash_all
import amazgone.shared.generated.resources.flash_title
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.presentation.components.CategoryTile
import com.cikup.amazgone.catalog.presentation.components.FlashProductCard
import com.cikup.amazgone.catalog.presentation.components.categoryIcon
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.fullBleed
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FlashSaleRoute(onBack: () -> Unit, onOpenProduct: (String, String) -> Unit, viewModel: FlashSaleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FlashSaleEffect.NavigateBack -> onBack()
                is FlashSaleEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
            }
        }
    }
    FlashSaleScreen(state, viewModel::onIntent)
}

@Composable
fun FlashSaleScreen(state: FlashSaleState, onIntent: (FlashSaleIntent) -> Unit) {
    val grid = rememberLazyGridState()
    val collapsed by remember { derivedStateOf { grid.firstVisibleItemIndex > 0 } }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = grid,
                columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
                contentPadding = PaddingValues(start = AmazgoneDimens.spaceLg, end = AmazgoneDimens.spaceLg, bottom = padding.calculateBottomPadding() + AmazgoneDimens.spaceXl),
                horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Always present (even before data) so the grid never anchors below an inserted header.
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    val sale = state.sale
                    if (sale == null) {
                        Spacer(Modifier.fullBleed(AmazgoneDimens.spaceLg).statusBarsPadding().height(AmazgoneDimens.iconXl * 2))
                    } else {
                        FlashSaleHeader(
                            modifier = Modifier.fullBleed(AmazgoneDimens.spaceLg),
                            sale = sale,
                            now = state.now,
                            scroll = { if (grid.firstVisibleItemIndex == 0) grid.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE },
                            onBack = { onIntent(FlashSaleIntent.Back) },
                        )
                    }
                }
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        modifier = Modifier.fullBleed(AmazgoneDimens.spaceLg),
                        contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceLg),
                        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                    ) {
                        item(key = "all") {
                            CategoryTile(Icons.Outlined.Bolt, stringResource(Res.string.flash_all), state.selectedCategory == null, { onIntent(FlashSaleIntent.SelectCategory(null)) })
                        }
                        items(state.categories, key = { it }) { slug ->
                            CategoryTile(categoryIcon(slug), Formatters.categoryLabel(slug), state.selectedCategory == slug, { onIntent(FlashSaleIntent.SelectCategory(slug)) })
                        }
                    }
                }
                itemsIndexed(state.visibleDeals, key = { _, d -> d.product.id }) { index, deal ->
                    FlashProductCard(
                        deal = deal,
                        origin = FLASH_SCREEN_ORIGIN,
                        saved = deal.product.id in state.savedIds,
                        onClick = { onIntent(FlashSaleIntent.OpenProduct(deal.product.id)) },
                        onToggleSaved = { onIntent(FlashSaleIntent.ToggleSaved(deal.product.id)) },
                        modifier = Modifier.animateItem().staggeredEnter(index),
                    )
                }
            }
            AnimatedVisibility(collapsed, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
                BackTopBar(stringResource(Res.string.flash_title), { onIntent(FlashSaleIntent.Back) })
            }
        }
    }
}
