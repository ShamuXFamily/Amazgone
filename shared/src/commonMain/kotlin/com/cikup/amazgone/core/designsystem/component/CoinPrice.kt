package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.coins_amount
import amazgone.shared.generated.resources.discount_off
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

/** Coin icon + amount. With [animateChanges] the value tweens (count up/down) when it changes. */
@Composable
fun CoinAmount(
    coins: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    iconSize: Dp = AmazgoneDimens.iconSm,
    animateChanges: Boolean = false,
) {
    val reduceMotion = LocalReduceMotion.current
    val shown by animateIntAsState(
        targetValue = coins.toInt(),
        animationSpec = if (animateChanges && !reduceMotion) tween(MotionTokens.DURATION_LONG_MS) else tween(0),
        label = "coinCount",
    )
    val description = stringResource(Res.string.coins_amount, Formatters.coins(coins))
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        Icon(Icons.Rounded.Toll, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(iconSize))
        Text(Formatters.coins(shown.toLong()), style = style)
    }
}

/** Current price, optional struck-through original price. */
@Composable
fun PriceBlock(
    priceCoins: Long,
    originalPriceCoins: Long?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
    ) {
        CoinAmount(priceCoins, style = style.copy(color = AmazgoneTheme.extended.cta, fontWeight = FontWeight.Bold))
        if (originalPriceCoins != null && originalPriceCoins > priceCoins) {
            Text(
                text = Formatters.coins(originalPriceCoins),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
            )
        }
    }
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    if (percent <= 0) return
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(Res.string.discount_off, percent),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2),
        )
    }
}

/** Integer that rolls vertically when it changes (cart badge, quantity stepper). */
@Composable
fun RollingNumber(value: Int, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.labelLarge) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically { if (up) it else -it } + fadeIn())
                .togetherWith(slideOutVertically { if (up) -it else it } + fadeOut())
        },
        label = "rollingNumber",
    ) { Text(it.toString(), style = style) }
}

private const val SECONDARY_ALPHA = 0.7f
