package com.cikup.amazgone.cart.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_browse
import amazgone.shared.generated.resources.cart_empty_body
import amazgone.shared.generated.resources.cart_empty_title
import amazgone.shared.generated.resources.cart_item_removed
import amazgone.shared.generated.resources.cart_items_count
import amazgone.shared.generated.resources.cart_saved_for_later
import amazgone.shared.generated.resources.cart_title
import amazgone.shared.generated.resources.cart_undo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CartRoute(
    onOpenProduct: (productId: String, origin: String) -> Unit,
    onCheckout: () -> Unit,
    onBrowse: () -> Unit,
    onPlayGames: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CartEffect.ShowUndo -> {
                    val result = snackbar.showSnackbar(
                        message = getString(Res.string.cart_item_removed, effect.title),
                        actionLabel = getString(Res.string.cart_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(CartIntent.Undo(effect.entry))
                }
                is CartEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
                CartEffect.NavigateToCheckout -> onCheckout()
                CartEffect.NavigateHome -> onBrowse()
                CartEffect.NavigateToGames -> onPlayGames()
                is CartEffect.SavedForLater -> snackbar.showSnackbar(getString(Res.string.cart_saved_for_later, effect.title))
            }
        }
    }
    CartScreen(state, viewModel::onIntent, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(state: CartState, onIntent: (CartIntent) -> Unit, snackbar: SnackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CartTopBar(state.summary.itemCount) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(
                !state.summary.isEmpty,
                enter = slideInVertically(MotionTokens.snappy()) { it } + fadeIn(),
                exit = slideOutVertically(MotionTokens.snappy()) { it } + fadeOut(),
            ) { CheckoutBar(state.summary) { onIntent(CartIntent.Checkout) } }
        },
    ) { padding ->
        if (!state.isLoading && state.summary.isEmpty) {
            MessageState(
                icon = Icons.Outlined.RemoveShoppingCart,
                title = stringResource(Res.string.cart_empty_title),
                body = stringResource(Res.string.cart_empty_body),
                actionLabel = stringResource(Res.string.cart_browse),
                onAction = { onIntent(CartIntent.BrowseDeals) },
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
        } else {
            CartList(state, onIntent, padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartTopBar(itemCount: Int) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Text(stringResource(Res.string.cart_title))
                AnimatedVisibility(itemCount > 0, enter = scaleIn(MotionTokens.bouncy()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Surface(color = AmazgoneTheme.extended.cta.copy(alpha = CHIP_ALPHA), contentColor = AmazgoneTheme.extended.cta, shape = CircleShape) {
                        Text(
                            pluralStringResource(Res.plurals.cart_items_count, itemCount, itemCount),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs),
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun CartList(state: CartState, onIntent: (CartIntent) -> Unit, padding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = AmazgoneDimens.spaceLg,
            end = AmazgoneDimens.spaceLg,
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + AmazgoneDimens.spaceLg,
        ),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        modifier = Modifier.fillMaxSize(),
    ) {
        state.balanceCoins?.let { balance ->
            item(key = "wallet") {
                WalletCheckCard(
                    balance = balance,
                    total = state.summary.totalCoins,
                    shortfall = state.shortfallCoins,
                    onPlay = { onIntent(CartIntent.PlayForCoins) },
                    modifier = Modifier.animateItem().staggeredEnter(0),
                )
            }
        }
        itemsIndexed(state.summary.lines, key = { _, line -> line.product.id }) { index, line ->
            CartLineRow(line, onIntent, Modifier.animateItem().staggeredEnter(index + 1))
        }
        if (!state.summary.isEmpty) {
            item(key = "summary") { OrderSummaryCard(state.summary, Modifier.animateItem().staggeredEnter(state.summary.lines.size + 1)) }
        }
    }
}

private const val CHIP_ALPHA = 0.15f
