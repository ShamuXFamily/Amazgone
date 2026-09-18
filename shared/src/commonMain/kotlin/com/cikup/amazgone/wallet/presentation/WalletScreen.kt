package com.cikup.amazgone.wallet.presentation

import com.cikup.amazgone.core.designsystem.component.appCardColors
import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.ledger_pending
import amazgone.shared.generated.resources.ledger_rejected
import amazgone.shared.generated.resources.wallet_balance
import amazgone.shared.generated.resources.wallet_empty
import amazgone.shared.generated.resources.wallet_history
import amazgone.shared.generated.resources.wallet_pending
import amazgone.shared.generated.resources.wallet_title
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.wallet.domain.model.LedgerEntry
import com.cikup.amazgone.wallet.domain.model.LedgerStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val COIN_FLIPS = 2

@Composable
fun WalletRoute(onBack: () -> Unit, viewModel: WalletViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.wallet_title), { viewModel.onIntent(WalletIntent.Back) }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item { BalanceHeader(state) }
            item { Text(stringResource(Res.string.wallet_history), style = MaterialTheme.typography.titleLarge) }
            if (!state.isLoading && state.summary.recent.isEmpty()) {
                item { Text(stringResource(Res.string.wallet_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            itemsIndexed(state.summary.recent, key = { _, e -> e.id }) { index, entry -> LedgerRow(entry, Modifier.animateItem().staggeredEnter(index)) }
        }
    }
}

/** A coin flips in (3D Y rotation) while the balance counts up. */
@Composable
private fun BalanceHeader(state: WalletState) {
    val reduceMotion = LocalReduceMotion.current
    val flip = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) flip.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedDecelerate)) }
    ElevatedCard(Modifier.fillMaxWidth(), colors = appCardColors()) {
        Column(
            Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
        ) {
            Icon(
                Icons.Rounded.Toll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(AmazgoneDimens.iconXl).graphicsLayer {
                    rotationY = (1 - flip.value) * FULL_TURN * COIN_FLIPS
                    cameraDistance = CAMERA_DISTANCE * density
                },
            )
            Text(stringResource(Res.string.wallet_balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoinAmount(state.summary.coins, style = MaterialTheme.typography.displaySmall, iconSize = AmazgoneDimens.iconMd, animateChanges = true)
            if (state.summary.pendingCoins != 0L) {
                Text(stringResource(Res.string.wallet_pending, Formatters.coins(state.summary.pendingCoins)), color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

private const val FULL_TURN = 360f
private const val CAMERA_DISTANCE = 12f

@Composable
private fun LedgerRow(entry: LedgerEntry, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(ledgerLabel(entry.reason)) },
        supportingContent = when (entry.status) {
            LedgerStatus.PENDING -> { { Text(stringResource(Res.string.ledger_pending), color = MaterialTheme.colorScheme.tertiary) } }
            LedgerStatus.REJECTED -> { { Text(stringResource(Res.string.ledger_rejected), color = MaterialTheme.colorScheme.error) } }
            LedgerStatus.CONFIRMED -> null
        },
        trailingContent = { CoinAmount(entry.amount, style = MaterialTheme.typography.titleSmall) },
        modifier = modifier,
    )
}
