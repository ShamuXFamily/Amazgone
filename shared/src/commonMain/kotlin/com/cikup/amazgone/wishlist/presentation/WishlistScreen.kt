package com.cikup.amazgone.wishlist.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.wishlist_empty_body
import amazgone.shared.generated.resources.wishlist_empty_title
import amazgone.shared.generated.resources.wishlist_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.ProductCard
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WishlistRoute(onBack: () -> Unit, onOpenProduct: (String, String) -> Unit, viewModel: WishlistViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WishlistEffect.NavigateBack -> onBack()
                is WishlistEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
            }
        }
    }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.wishlist_title), { viewModel.onIntent(WishlistIntent.Back) }) }) { padding ->
        if (!state.isLoading && state.products.isEmpty()) {
            MessageState(
                Icons.Outlined.FavoriteBorder,
                stringResource(Res.string.wishlist_empty_title),
                stringResource(Res.string.wishlist_empty_body),
                Modifier.padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
                contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                itemsIndexed(state.products, key = { _, p -> p.id }) { index, product ->
                    Box(Modifier.animateItem().staggeredEnter(index)) {
                        ProductCard(product, WISHLIST_ORIGIN, onClick = { viewModel.onIntent(WishlistIntent.OpenProduct(product.id)) })
                        HeartButton(saved = true, onToggle = { viewModel.onIntent(WishlistIntent.Remove(product.id)) }, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
    }
}
