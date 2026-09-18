package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import amazgone.shared.generated.resources.checkout_continue
import amazgone.shared.generated.resources.checkout_hold_to_pay
import amazgone.shared.generated.resources.checkout_insufficient
import amazgone.shared.generated.resources.checkout_step_address
import amazgone.shared.generated.resources.checkout_step_payment
import amazgone.shared.generated.resources.checkout_step_review
import amazgone.shared.generated.resources.checkout_title
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.motion.HoldToConfirmButton
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.shakeOnChange
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.error.message
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CheckoutRoute(
    onClose: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onGoHome: () -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckoutEffect.Close -> onClose()
                is CheckoutEffect.OpenOrder -> onOpenOrder(effect.orderId)
                CheckoutEffect.GoHome -> onGoHome()
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.checkout_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CheckoutIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back))
                    }
                },
            )
        },
        bottomBar = { CheckoutBottomBar(state, onIntent) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StepIndicator(state.step, Modifier.padding(horizontal = AmazgoneDimens.spaceLg))
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(MotionTokens.gentle()) { if (forward) it else -it } + fadeIn())
                        .togetherWith(slideOutHorizontally(MotionTokens.gentle()) { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                modifier = Modifier.weight(1f).shakeOnChange(state.errorPulse),
                label = "checkoutStep",
            ) { step ->
                when (step) {
                    CheckoutStep.ADDRESS -> AddressStep(state, onIntent)
                    CheckoutStep.PAYMENT -> PaymentStep(state, onIntent)
                    CheckoutStep.REVIEW -> ReviewStep(state)
                }
            }
        }
    }
}

/** Three segments; the active one widens and all completed ones fill with a spring. */
@Composable
private fun StepIndicator(step: CheckoutStep, modifier: Modifier = Modifier) {
    val labels = listOf(Res.string.checkout_step_address, Res.string.checkout_step_payment, Res.string.checkout_step_review)
    Row(modifier.fillMaxWidth().padding(vertical = AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        CheckoutStep.entries.forEachIndexed { index, s ->
            val done = s.ordinal <= step.ordinal
            val weight by animateFloatAsState(if (s == step) ACTIVE_WEIGHT else 1f, MotionTokens.bouncy(), label = "stepWeight")
            val color by animateColorAsState(
                if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "stepColor",
            )
            Column(Modifier.weight(weight), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Box(Modifier.fillMaxWidth().height(AmazgoneDimens.spaceXs * 1.5f).clip(MaterialTheme.shapes.small).background(color))
                Text(
                    stringResource(labels[index]),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val ACTIVE_WEIGHT = 1.6f

@Composable
private fun CheckoutBottomBar(state: CheckoutState, onIntent: (CheckoutIntent) -> Unit) {
    Surface(tonalElevation = AmazgoneDimens.spaceXs) {
        Box(Modifier.navigationBarsPadding().padding(AmazgoneDimens.spaceLg), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = state.step == CheckoutStep.REVIEW, label = "cta") { review ->
                if (review) {
                    HoldToConfirmButton(
                        label = stringResource(Res.string.checkout_hold_to_pay, Formatters.coins(state.summary.totalCoins)),
                        onConfirm = { onIntent(CheckoutIntent.Pay) },
                        enabled = state.canAfford && !state.isPlacing,
                    )
                } else {
                    Button(onClick = { onIntent(CheckoutIntent.Next) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.checkout_continue))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ErrorBanner(state: CheckoutState) {
    AnimatedVisibility(state.error != null || !state.canAfford) {
        val error = state.error
        Text(
            text = if (error != null) {
                error.message()
            } else {
                stringResource(Res.string.checkout_insufficient, Formatters.coins(-state.balanceAfter))
            },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
