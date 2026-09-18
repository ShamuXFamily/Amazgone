package com.cikup.amazgone.orders.presentation.checkout

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_keep_shopping
import amazgone.shared.generated.resources.checkout_success_body
import amazgone.shared.generated.resources.checkout_success_title
import amazgone.shared.generated.resources.checkout_view_order
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import com.cikup.amazgone.core.designsystem.motion.AnimatedCheckmark
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.orders.domain.model.Order
import org.jetbrains.compose.resources.stringResource

/** Celebration: check draws itself, confetti bursts, content cascades in. */
@Composable
fun OrderPlacedScreen(order: Order, onIntent: (CheckoutIntent) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(order.id) { haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    Surface(Modifier.fillMaxSize()) {
        Box {
            Column(
                Modifier.fillMaxSize().safeContentPadding().padding(AmazgoneDimens.spaceXl),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedCheckmark(MaterialTheme.colorScheme.primary, Modifier.size(AmazgoneDimens.iconXl * 1.5f))
                Text(stringResource(Res.string.checkout_success_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.staggeredEnter(2))
                Text(
                    stringResource(Res.string.checkout_success_body, Formatters.coins(order.xpEarned)),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.staggeredEnter(3),
                )
                Button(onClick = { onIntent(CheckoutIntent.ViewOrder) }, modifier = Modifier.fillMaxWidth().staggeredEnter(4)) {
                    Text(stringResource(Res.string.checkout_view_order))
                }
                OutlinedButton(onClick = { onIntent(CheckoutIntent.KeepShopping) }, modifier = Modifier.fillMaxWidth().staggeredEnter(5)) {
                    Text(stringResource(Res.string.checkout_keep_shopping))
                }
            }
            ConfettiBurst(trigger = order.id)
        }
    }
}
