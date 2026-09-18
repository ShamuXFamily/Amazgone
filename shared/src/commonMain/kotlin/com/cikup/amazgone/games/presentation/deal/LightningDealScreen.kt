package com.cikup.amazgone.games.presentation.deal

import com.cikup.amazgone.core.designsystem.component.appCardColors
import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_deal_body
import amazgone.shared.generated.resources.game_deal_claim
import amazgone.shared.generated.resources.game_deal_claimed
import amazgone.shared.generated.resources.game_deal_claimed_toast
import amazgone.shared.generated.resources.game_deal_ends_in
import amazgone.shared.generated.resources.game_deal_none
import amazgone.shared.generated.resources.game_deal_title
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.cikup.amazgone.core.designsystem.component.PriceBlock
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.games.presentation.formatCountdown
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val URGENT_MS = 60_000L
private const val URGENT_SCALE = 1.03f
private const val PERCENT = 100

@Composable
fun LightningDealRoute(onBack: () -> Unit, onOpenProduct: (String) -> Unit, viewModel: DealViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DealEffect.Claimed -> snackbar.showSnackbar(getString(Res.string.game_deal_claimed_toast, effect.couponCode))
                is DealEffect.NavigateToProduct -> onOpenProduct(effect.productId)
                DealEffect.NavigateBack -> onBack()
            }
        }
    }
    Scaffold(
        topBar = { BackTopBar(stringResource(Res.string.game_deal_title), { viewModel.onIntent(DealIntent.Back) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val deal = state.deal
        if (deal == null) {
            Text(stringResource(Res.string.game_deal_none), modifier = Modifier.padding(padding).padding(AmazgoneDimens.spaceXl))
        } else {
            DealContent(state, viewModel::onIntent, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DealContent(state: DealState, onIntent: (DealIntent) -> Unit, modifier: Modifier) {
    val deal = state.deal ?: return
    val urgent = state.remaining in 1 until URGENT_MS && !LocalReduceMotion.current
    val pulse = if (urgent) {
        rememberInfiniteTransition(label = "urgent").animateFloat(
            1f, URGENT_SCALE, infiniteRepeatable(tween(MotionTokens.DURATION_LONG_MS), RepeatMode.Reverse), label = "urgentScale",
        ).value
    } else {
        1f
    }
    val claimed by animateFloatAsState(deal.claimedFraction(state.now), MotionTokens.gentle(), label = "claimed")
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
    ) {
        ElevatedCard(onClick = { onIntent(DealIntent.OpenProduct) }, modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = pulse; scaleY = pulse }, colors = appCardColors()) {
            ProductImage(deal.product.imageUrls.firstOrNull() ?: deal.product.thumbnailUrl, deal.product.id, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Text(stringResource(Res.string.game_deal_body, deal.extraPercent.toInt()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                Text(deal.product.title, style = MaterialTheme.typography.titleLarge)
                PriceBlock(state.dealPriceCoins, deal.product.priceCoins, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Column(Modifier.staggeredEnter(1), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Text(stringResource(Res.string.game_deal_ends_in), style = MaterialTheme.typography.labelLarge)
            FlipCountdown(formatCountdown(state.remaining))
            LinearProgressIndicator(progress = { claimed }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.game_deal_claimed, (claimed * PERCENT).toInt()), style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { onIntent(DealIntent.Claim) }, enabled = !state.isClaiming, modifier = Modifier.fillMaxWidth().staggeredEnter(2)) {
            Text(stringResource(Res.string.game_deal_claim))
        }
    }
}

/** Each digit flips (slides) independently when it changes, like an airport board. */
@Composable
private fun FlipCountdown(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
        text.forEach { char ->
            if (char == ':') {
                Text(":", style = MaterialTheme.typography.headlineMedium)
            } else {
                Surface(color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface, shape = MaterialTheme.shapes.small) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = { (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut()) },
                        label = "digit",
                    ) { digit ->
                        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm))
                    }
                }
            }
        }
    }
}
