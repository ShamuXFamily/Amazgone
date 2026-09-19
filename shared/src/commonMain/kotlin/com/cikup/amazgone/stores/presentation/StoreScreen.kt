package com.cikup.amazgone.stores.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import amazgone.shared.generated.resources.store_all
import amazgone.shared.generated.resources.store_not_found
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.presentation.search.sortLabel
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.fullBleed
import com.cikup.amazgone.core.designsystem.component.ProductCard
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun StoreRoute(
    storeId: String,
    onBack: () -> Unit,
    onOpenProduct: (productId: String, origin: String) -> Unit,
    viewModel: StoreViewModel = koinViewModel(key = "store/$storeId") { parametersOf(storeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StoreEffect.NavigateBack -> onBack()
                is StoreEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
            }
        }
    }
    StoreScreen(state, viewModel::onIntent)
}

@Composable
fun StoreScreen(state: StoreState, onIntent: (StoreIntent) -> Unit) {
    val grid = rememberLazyGridState()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box {
            val summary = state.summary
            when {
                summary != null -> LazyVerticalGrid(
                    state = grid,
                    columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
                    contentPadding = PaddingValues(start = AmazgoneDimens.spaceLg, end = AmazgoneDimens.spaceLg, bottom = AmazgoneDimens.iconXl + AmazgoneDimens.spaceXl),
                    horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                ) {
                    fullWidth("hero") { StoreHero(summary, Modifier.fullBleed(AmazgoneDimens.spaceLg)) }
                    fullWidth("filters") { Filters(state, onIntent, Modifier.fullBleed(AmazgoneDimens.spaceLg)) }
                    products(state, onIntent)
                }
                state.notFound -> MessageState(Icons.Outlined.Storefront, stringResource(Res.string.store_not_found), "", Modifier.fillMaxSize())
            }
            StoreTopBar(state, grid, onIntent)
        }
    }
}

private fun LazyGridScope.products(state: StoreState, onIntent: (StoreIntent) -> Unit) {
    itemsIndexed(state.products, key = { _, p -> p.id }) { index, product ->
        ProductCard(
            product = product,
            origin = STORE_ORIGIN,
            onClick = { onIntent(StoreIntent.OpenProduct(product.id)) },
            modifier = Modifier.animateItem().staggeredEnter(index),
        )
    }
}

@Composable
private fun Filters(state: StoreState, onIntent: (StoreIntent) -> Unit, modifier: Modifier) {
    val selectedColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = AmazgoneTheme.extended.cta.copy(alpha = CHIP_ALPHA),
        selectedLabelColor = AmazgoneTheme.extended.cta,
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        if (state.categories.size > 1) {
            LazyRow(contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                item {
                    FilterChip(state.selectedCategory == null, { onIntent(StoreIntent.SelectCategory(null)) }, { Text(stringResource(Res.string.store_all)) }, colors = selectedColors)
                }
                items(state.categories) { slug ->
                    FilterChip(state.selectedCategory == slug, { onIntent(StoreIntent.SelectCategory(slug)) }, { Text(Formatters.categoryLabel(slug)) }, colors = selectedColors)
                }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            items(STORE_SORTS) { sort ->
                FilterChip(state.sort == sort, { onIntent(StoreIntent.ChangeSort(sort)) }, { Text(sortLabel(sort)) }, colors = selectedColors)
            }
        }
    }
}

/** Floating back button over the banner; once the banner scrolls away a solid bar with the store name slides in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreTopBar(state: StoreState, grid: LazyGridState, onIntent: (StoreIntent) -> Unit) {
    val collapsed by remember(grid) { derivedStateOf { grid.firstVisibleItemIndex > 0 } }
    AnimatedVisibility(!collapsed, enter = fadeIn(), exit = fadeOut()) {
        FilledTonalIconButton(
            onClick = { onIntent(StoreIntent.Back) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.statusBarsPadding().padding(AmazgoneDimens.spaceSm),
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back)) }
    }
    AnimatedVisibility(
        visible = collapsed,
        enter = slideInVertically(MotionTokens.snappy()) { -it } + fadeIn(),
        exit = slideOutVertically(MotionTokens.snappy()) { -it } + fadeOut(),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = AmazgoneDimens.spaceXs) {
            TopAppBar(
                title = {
                    state.summary?.let { summary ->
                        Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                            StoreLogo(summary.store, AmazgoneDimens.iconMd + AmazgoneDimens.spaceXs)
                            Text(summary.store.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            if (summary.store.isVerified) VerifiedIcon()
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(StoreIntent.Back) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            )
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private const val CHIP_ALPHA = 0.15f
