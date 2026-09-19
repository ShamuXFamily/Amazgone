package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import amazgone.shared.generated.resources.checkout_hold_to_pay
import amazgone.shared.generated.resources.checkout_title_address
import amazgone.shared.generated.resources.checkout_title_review
import amazgone.shared.generated.resources.checkout_total
import amazgone.shared.generated.resources.checkout_use_address
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.HoldToConfirmButton
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.shakeOnChange
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CheckoutRoute(
    onClose: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onGoHome: () -> Unit,
    onOpenGarage: () -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckoutEffect.Close -> onClose()
                is CheckoutEffect.OpenOrder -> onOpenOrder(effect.orderId)
                CheckoutEffect.GoHome -> onGoHome()
                CheckoutEffect.OpenGarage -> onOpenGarage()
            }
        }
    }
    val order = state.placedOrder
    if (order != null) {
        OrderPlacedScreen(order, onIntent = viewModel::onIntent)
    } else {
        CheckoutScreen(state, viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.errorPulse) { if (state.errorPulse > 0) haptics.performHapticFeedback(HapticFeedbackType.Reject) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(state.step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") { step ->
                        Text(stringResource(if (step == CheckoutStep.ADDRESS) Res.string.checkout_title_address else Res.string.checkout_title_review))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CheckoutIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { if (state.addressLoaded) CheckoutBottomBar(state, onIntent) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // Until the last address is looked up we show nothing rather than flashing the empty form.
            if (state.addressLoaded) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(MotionTokens.gentle()) { if (forward) it else -it } + fadeIn())
                            .togetherWith(slideOutHorizontally(MotionTokens.gentle()) { if (forward) -it / 3 else it / 3 } + fadeOut())
                    },
                    modifier = Modifier.fillMaxSize().shakeOnChange(state.errorPulse),
                    label = "checkoutStep",
                ) { step ->
                    when (step) {
                        CheckoutStep.ADDRESS -> AddressStep(state, onIntent)
                        CheckoutStep.REVIEW -> ReviewStep(state, onIntent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutBottomBar(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    val ext = AmazgoneTheme.extended
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = AmazgoneDimens.spaceXl, topEnd = AmazgoneDimens.spaceXl),
        shadowElevation = AmazgoneDimens.spaceSm,
    ) {
        Box(Modifier.navigationBarsPadding().padding(AmazgoneDimens.spaceLg), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = state.step == CheckoutStep.REVIEW, label = "cta") { review ->
                if (review) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                        Column {
                            Text(stringResource(Res.string.checkout_total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CoinAmount(state.totalCoins, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), animateChanges = true)
                        }
                        HoldToConfirmButton(
                            label = stringResource(Res.string.checkout_hold_to_pay, Formatters.coins(state.totalCoins)),
                            onConfirm = { onIntent(CheckoutIntent.Pay) },
                            enabled = state.canAfford && !state.isPlacing,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Button(
                        onClick = { onIntent(CheckoutIntent.Next) },
                        colors = ButtonDefaults.buttonColors(containerColor = ext.cta, contentColor = ext.onCta),
                        modifier = Modifier.fillMaxWidth().heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm),
                    ) { Text(stringResource(Res.string.checkout_use_address), style = MaterialTheme.typography.titleSmall) }
                }
            }
        }
    }
}
