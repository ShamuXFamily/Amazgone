package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_also_bought
import amazgone.shared.generated.resources.detail_limit_reached
import amazgone.shared.generated.resources.detail_not_found
import amazgone.shared.generated.resources.detail_reviews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.Review
import com.cikup.amazgone.core.designsystem.component.CompactProductCard
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.SectionHeader
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.navigation.LocalFlyToCart
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductDetailRoute(
    productId: String,
    origin: String,
    onBack: () -> Unit,
    onOpenProduct: (productId: String, origin: String) -> Unit,
    onOpenStore: (storeId: String) -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(key = "$origin/$productId") { parametersOf(productId, origin) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val flyToCart = LocalFlyToCart.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var heroBounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductDetailEffect.NavigateBack -> onBack()
                is ProductDetailEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
                is ProductDetailEffect.NavigateToStore -> onOpenStore(effect.storeId)
                is ProductDetailEffect.FlyToCart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    heroBounds?.let { flyToCart.launch(effect.productId, effect.imageUrl, it) }
                }
                ProductDetailEffect.LimitReached -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    snackbar.showSnackbar(getString(Res.string.detail_limit_reached))
                }
            }
        }
    }
    ProductDetailScreen(state, viewModel::onIntent, snackbar, onHeroPositioned = { heroBounds = it })
}

@Composable
fun ProductDetailScreen(
    state: ProductDetailState,
    onIntent: (ProductDetailIntent) -> Unit,
    snackbar: SnackbarHostState,
    onHeroPositioned: (Rect) -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { state.product?.let { BuyBar(it, state, onIntent) } },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            val product = state.product
            when {
                product != null -> DetailContent(product, state, onIntent, listState, padding, onHeroPositioned)
                state.notFound -> MessageState(Icons.Outlined.Inventory2, stringResource(Res.string.detail_not_found), "", Modifier.align(Alignment.Center))
            }
            DetailOverlay(product, rememberIsCollapsed(listState), state.isSaved, onIntent)
        }
    }
}

@Composable
private fun DetailContent(
    product: Product,
    state: ProductDetailState,
    onIntent: (ProductDetailIntent) -> Unit,
    listState: LazyListState,
    padding: PaddingValues,
    onHeroPositioned: (Rect) -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + AmazgoneDimens.spaceXxl),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "hero") {
            ImageStage(
                product = product,
                origin = state.origin,
                scrollOffset = { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 0 },
                modifier = Modifier.onGloballyPositioned { onHeroPositioned(it.boundsInRoot()) },
            )
        }
        item(key = "info") { InfoCard(product, state, onIntent) }
        if (product.details.highlights.isNotEmpty()) item(key = "highlights") { Highlights(product) }
        item(key = "facts") { KeyFacts(product) }
        if (product.details.imageCredit != null) item(key = "credit") { ImageCredit(product) }
        item(key = "reviews-title") { SectionHeader(stringResource(Res.string.detail_reviews)) }
        if (state.allReviews.isEmpty() && product.rating == null) {
            item(key = "no-reviews") { NoReviews() }
        } else {
            item(key = "rating-summary") { RatingSummary(product.rating, state.allReviews, Modifier.staggeredEnter(0)) }
            itemsIndexed(state.shopperReviews, key = { _, review -> "shopper-${review.productId}-${review.authorName}-${review.createdAt}" }) { index, review ->
                ReviewCard(Review(review.productId, review.authorName, review.rating, review.comment, review.createdAt), Modifier.staggeredEnter(index), mine = review.isMine)
            }
            itemsIndexed(state.reviews, key = { index, _ -> "review-$index" }) { index, review -> ReviewCard(review, Modifier.staggeredEnter(index + state.shopperReviews.size)) }
        }
        if (state.recommendations.isNotEmpty()) {
            item(key = "also-title") { SectionHeader(stringResource(Res.string.detail_also_bought)) }
            item(key = "also") {
                LazyRow(contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    items(state.recommendations, key = { it.id }) { item ->
                        CompactProductCard(item, RECOMMENDATION_ORIGIN, onClick = { onIntent(ProductDetailIntent.OpenProduct(item.id)) })
                    }
                }
            }
        }
    }
}
