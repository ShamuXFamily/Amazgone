package com.cikup.amazgone.cart.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.cart_browse
import amazgone.shared.generated.resources.cart_checkout
import amazgone.shared.generated.resources.cart_empty_body
import amazgone.shared.generated.resources.cart_empty_title
import amazgone.shared.generated.resources.cart_item_removed
import amazgone.shared.generated.resources.cart_savings
import amazgone.shared.generated.resources.cart_subtotal
import amazgone.shared.generated.resources.cart_title
import amazgone.shared.generated.resources.cart_undo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CartRoute(
    onOpenProduct: (productId: String, origin: String) -> Unit,
    onCheckout: () -> Unit,
    onBrowse: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.cart_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(!state.summary.isEmpty) { CheckoutBar(state.summary) { onIntent(CartIntent.Checkout) } }
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
                items(state.summary.lines, key = { it.product.id }) { line ->
                    CartLineRow(line, onIntent, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun CheckoutBar(summary: CartSummary, onCheckout: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = AmazgoneDimens.spaceXl, topEnd = AmazgoneDimens.spaceXl),
        shadowElevation = AmazgoneDimens.spaceSm,
    ) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.cart_subtotal, summary.itemCount), style = MaterialTheme.typography.titleMedium)
                CoinAmount(summary.subtotalCoins, style = MaterialTheme.typography.titleLarge, animateChanges = true)
            }
            if (summary.savingsCoins > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.cart_savings), color = MaterialTheme.colorScheme.error)
                    CoinAmount(summary.savingsCoins, style = MaterialTheme.typography.bodyMedium, animateChanges = true)
                }
            }
            Button(
                onClick = onCheckout,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta),
                modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm).pressScale(interaction),
            ) {
                Text(stringResource(Res.string.cart_checkout))
            }
        }
    }
}
