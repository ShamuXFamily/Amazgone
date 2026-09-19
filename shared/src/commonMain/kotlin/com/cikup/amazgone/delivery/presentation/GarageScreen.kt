package com.cikup.amazgone.delivery.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.garage_confirm_body
import amazgone.shared.generated.resources.garage_confirm_no
import amazgone.shared.generated.resources.garage_confirm_title
import amazgone.shared.generated.resources.garage_confirm_yes
import amazgone.shared.generated.resources.garage_sample
import amazgone.shared.generated.resources.garage_short
import amazgone.shared.generated.resources.garage_title
import amazgone.shared.generated.resources.garage_unlocked
import amazgone.shared.generated.resources.garage_unlocked_toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.delivery.domain.model.Courier
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GarageRoute(onBack: () -> Unit, viewModel: GarageViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GarageEffect.NavigateBack -> onBack()
                is GarageEffect.Unlocked -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(getString(Res.string.garage_unlocked_toast, effect.courier.emoji, getString(courierNameRes(effect.courier))))
                }
                is GarageEffect.NotEnoughCoins -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    snackbar.showSnackbar(getString(Res.string.garage_short, Formatters.coins(effect.shortfall)))
                }
            }
        }
    }
    GarageScreen(state, viewModel::onIntent, snackbar)
}

@Composable
fun GarageScreen(state: GarageState, onIntent: (GarageIntent) -> Unit, snackbar: SnackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(Res.string.garage_title), { onIntent(GarageIntent.Back) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = AmazgoneDimens.spaceLg,
                end = AmazgoneDimens.spaceLg,
                top = padding.calculateTopPadding() + AmazgoneDimens.spaceSm,
                bottom = AmazgoneDimens.iconXl + AmazgoneDimens.spaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") { GarageHeader(state, Modifier.staggeredEnter(0)) }
            itemsIndexed(Courier.entries, key = { _, c -> c.name }) { index, courier ->
                CourierTimelineRow(courier, state, isLast = index == Courier.entries.lastIndex, onIntent, Modifier.staggeredEnter(index + 1))
            }
        }
    }
    state.confirming?.let { courier -> ConfirmDialog(courier, state.isBuying, onIntent) }
}

@Composable
private fun GarageHeader(state: GarageState, modifier: Modifier) {
    val ext = AmazgoneTheme.extended
    val unlockedFraction by animateFloatAsState(state.owned.size / Courier.entries.size.toFloat(), MotionTokens.gentle(), label = "unlocked")
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, ext.cta, HEADER_ACCENT))))
            .padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.garage_unlocked, state.owned.size, Courier.entries.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ext.onBrandNavy, modifier = Modifier.weight(1f))
            CoinAmount(state.balance, style = MaterialTheme.typography.titleLarge.copy(color = ext.onBrandNavy, fontWeight = FontWeight.Bold), iconSize = AmazgoneDimens.iconMd, animateChanges = true)
        }
        // Progress of the collection, in the same pill style as the XP bar.
        Row(Modifier.fillMaxWidth().height(AmazgoneDimens.spaceSm).clip(CircleShape).background(ext.onBrandNavy.copy(alpha = TRACK_ALPHA))) {
            if (unlockedFraction > 0f) Row(Modifier.weight(unlockedFraction).height(AmazgoneDimens.spaceSm).background(ext.cta)) {}
            if (unlockedFraction < 1f) Row(Modifier.weight(1f - unlockedFraction)) {}
        }
        Text(
            stringResource(Res.string.garage_sample, state.sampleFrom, state.sampleTo, Formatters.coins(state.sampleKm.toLong())),
            style = MaterialTheme.typography.labelMedium,
            color = ext.onBrandNavyVariant,
        )
    }
}

@Composable
private fun ConfirmDialog(courier: Courier, busy: Boolean, onIntent: (GarageIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(GarageIntent.DismissBuy) },
        icon = { Text(courier.emoji, style = MaterialTheme.typography.displaySmall) },
        title = { Text(stringResource(Res.string.garage_confirm_title, courierName(courier))) },
        text = { Text(stringResource(Res.string.garage_confirm_body, Formatters.coins(courier.priceCoins))) },
        confirmButton = {
            TextButton(onClick = { onIntent(GarageIntent.ConfirmBuy) }, enabled = !busy) {
                Text(stringResource(Res.string.garage_confirm_yes), color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = { onIntent(GarageIntent.DismissBuy) }) { Text(stringResource(Res.string.garage_confirm_no)) } },
    )
}

private const val HEADER_ACCENT = 0.4f
private const val TRACK_ALPHA = 0.18f
