package com.cikup.amazgone.delivery.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.garage_confirm_body
import amazgone.shared.generated.resources.garage_confirm_no
import amazgone.shared.generated.resources.garage_confirm_title
import amazgone.shared.generated.resources.garage_confirm_yes
import amazgone.shared.generated.resources.garage_fastest
import amazgone.shared.generated.resources.garage_filter_all
import amazgone.shared.generated.resources.garage_filter_locked
import amazgone.shared.generated.resources.garage_filter_owned
import amazgone.shared.generated.resources.garage_fleet
import amazgone.shared.generated.resources.garage_sample
import amazgone.shared.generated.resources.garage_short
import amazgone.shared.generated.resources.garage_title
import amazgone.shared.generated.resources.garage_unlocked
import amazgone.shared.generated.resources.garage_unlocked_toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.presentation.format.durationText
import com.cikup.amazgone.delivery.domain.model.Courier
import kotlinx.coroutines.delay
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
        Box(Modifier.fillMaxSize()) {
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
                item(key = "hero") { FleetHero(state, Modifier.staggeredEnter(0)) }
                item(key = "filters") { Filters(state.filter, onIntent) }
                state.sections.forEach { (era, couriers) ->
                    item(key = "era-$era") { EraHeader(era, Modifier.animateItem()) }
                    itemsIndexed(couriers, key = { _, c -> c.name }) { index, courier ->
                        CourierCard(courier, state, isLastInEra = index == couriers.lastIndex, onIntent, Modifier.animateItem().staggeredEnter(index))
                    }
                }
            }
            state.justUnlocked?.let { courier ->
                ConfettiBurst(trigger = courier, origin = Offset(0.5f, CONFETTI_Y))
                LaunchedEffect(courier) {
                    delay(MotionTokens.DURATION_EXTRA_LONG_MS * 2L)
                    onIntent(GarageIntent.CelebrationDone)
                }
            }
        }
    }
    state.confirming?.let { courier -> ConfirmDialog(courier, state.isBuying, onIntent) }
}

/** Navy hero: the couriers you own as badges (new ones pop in), the collection bar, balance and your best trip time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FleetHero(state: GarageState, modifier: Modifier) {
    val ext = AmazgoneTheme.extended
    val collected by animateFloatAsState(state.owned.size / Courier.entries.size.toFloat(), MotionTokens.gentle(), label = "collected")
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(ext.brandNavy, lerp(ext.brandNavy, ext.cta, HEADER_ACCENT))))
            .padding(AmazgoneDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.garage_fleet), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ext.onBrandNavy)
                Text(stringResource(Res.string.garage_unlocked, state.owned.size, Courier.entries.size), style = MaterialTheme.typography.labelLarge, color = ext.onBrandNavyVariant)
            }
            Surface(color = ext.onBrandNavy.copy(alpha = CHIP_ALPHA), shape = CircleShape) {
                CoinAmount(
                    state.balance,
                    style = MaterialTheme.typography.titleMedium.copy(color = ext.onBrandNavy, fontWeight = FontWeight.Bold),
                    animateChanges = true,
                    modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceXs),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Courier.entries.filter { it in state.owned }.forEach { courier ->
                FleetBadge(courier, isNew = courier == state.justUnlocked)
            }
        }
        Box(Modifier.fillMaxWidth().height(AmazgoneDimens.spaceSm).clip(CircleShape).background(ext.onBrandNavy.copy(alpha = TRACK_ALPHA))) {
            Box(Modifier.fillMaxWidth(collected).height(AmazgoneDimens.spaceSm).clip(CircleShape).background(ext.cta))
        }
        state.fastestOwned?.let { fastest ->
            Text(
                "${fastest.emoji}  " + stringResource(Res.string.garage_fastest, state.sampleFrom, state.sampleTo, durationText(state.tripMillis(fastest) ?: 0)),
                style = MaterialTheme.typography.labelLarge,
                color = ext.onBrandNavy,
            )
        }
        Text(
            stringResource(Res.string.garage_sample, state.sampleFrom, state.sampleTo, Formatters.coins(state.sampleKm.toLong())),
            style = MaterialTheme.typography.labelSmall,
            color = ext.onBrandNavyVariant,
        )
    }
}

/** A soft disc with the courier emoji; a freshly unlocked one springs in. */
@Composable
private fun FleetBadge(courier: Courier, isNew: Boolean) {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isNew) {
        if (isNew && !reduceMotion) {
            scale.snapTo(0f)
            scale.animateTo(1f, MotionTokens.bouncy())
        }
    }
    Surface(
        // Translucent on navy: silver emoji such as ✈️ would vanish on a white disc.
        color = AmazgoneTheme.extended.onBrandNavy.copy(alpha = CHIP_ALPHA),
        shape = CircleShape,
        modifier = Modifier.size(BADGE).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        Box(contentAlignment = Alignment.Center) { Text(courier.emoji, style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun Filters(selected: GarageFilter, onIntent: (GarageIntent) -> Unit) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = AmazgoneTheme.extended.cta.copy(alpha = CHIP_ALPHA),
        selectedLabelColor = AmazgoneTheme.extended.cta,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        GarageFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onIntent(GarageIntent.SetFilter(filter)) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                GarageFilter.ALL -> Res.string.garage_filter_all
                                GarageFilter.OWNED -> Res.string.garage_filter_owned
                                GarageFilter.LOCKED -> Res.string.garage_filter_locked
                            },
                        ),
                    )
                },
                colors = colors,
            )
        }
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
private const val CHIP_ALPHA = 0.15f
private const val CONFETTI_Y = 0.25f
private val BADGE = AmazgoneDimens.iconLg * 0.8f
