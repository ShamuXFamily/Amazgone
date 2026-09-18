package com.cikup.amazgone.catalog.presentation.flash

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import amazgone.shared.generated.resources.flash_banner_body
import amazgone.shared.generated.resources.flash_banner_chip
import amazgone.shared.generated.resources.flash_banner_title
import amazgone.shared.generated.resources.flash_done
import amazgone.shared.generated.resources.flash_next
import amazgone.shared.generated.resources.flash_ongoing
import amazgone.shared.generated.resources.flash_title
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cikup.amazgone.catalog.domain.model.FlashSale
import com.cikup.amazgone.core.designsystem.component.RollingDigit
import com.cikup.amazgone.core.designsystem.component.pullUp
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.games.presentation.formatCountdown
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

private val HEADER_CURVE = 36.dp
private val BANNER_OVERLAP = 28.dp
private const val PARALLAX = 0.45f
private const val BOLT_PULSE = 1.25f

/** Curved orange header with done / ongoing / next slots; drifts and fades as the list scrolls. */
@Composable
fun FlashSaleHeader(sale: FlashSale, now: Long, scroll: () -> Float, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AmazgoneTheme.extended
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = scroll() * PARALLAX
                    alpha = 1f - (scroll() / size.height).coerceIn(0f, 1f)
                }
                .background(colors.cta, RoundedCornerShape(bottomStart = HEADER_CURVE, bottomEnd = HEADER_CURVE))
                .statusBarsPadding()
                .padding(bottom = BANNER_OVERLAP + AmazgoneDimens.spaceLg),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.onCta.copy(alpha = 0.2f), contentColor = colors.onCta),
                    ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back)) }
                    Text(
                        stringResource(Res.string.flash_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onCta,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.padding(horizontal = AmazgoneDimens.spaceXl))
                }
                Row(Modifier.fillMaxWidth().padding(top = AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Slot(clock(sale.previousStart), stringResource(Res.string.flash_done), Modifier.staggeredEnter(0))
                    OngoingSlot(formatCountdown(sale.remainingMillis(now)).drop(3), Modifier.staggeredEnter(1))
                    Slot(clock(sale.nextStart), stringResource(Res.string.flash_next), Modifier.staggeredEnter(2))
                }
            }
        }
        FlashBanner(sale, Modifier.pullUp(BANNER_OVERLAP).padding(horizontal = AmazgoneDimens.spaceLg).staggeredEnter(3))
    }
}

@Composable
private fun Slot(time: String, label: String, modifier: Modifier) {
    val colors = AmazgoneTheme.extended
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, style = MaterialTheme.typography.titleMedium, color = colors.onCta.copy(alpha = 0.8f))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onCta.copy(alpha = 0.8f))
    }
}

/** Big MM:SS where every digit rolls independently. */
@Composable
private fun OngoingSlot(minutesSeconds: String, modifier: Modifier) {
    val colors = AmazgoneTheme.extended
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            minutesSeconds.forEach { char ->
                val style = MaterialTheme.typography.displaySmall.copy(color = colors.onCta, fontWeight = FontWeight.Bold)
                if (char.isDigit()) RollingDigit(char, style) else Text(char.toString(), style = style)
            }
        }
        Text(stringResource(Res.string.flash_ongoing), style = MaterialTheme.typography.labelMedium, color = colors.onCta)
    }
}

/** Navy card overlapping the header; the bolt pulses to draw the eye. */
@Composable
private fun FlashBanner(sale: FlashSale, modifier: Modifier) {
    val colors = AmazgoneTheme.extended
    val pulse = if (LocalReduceMotion.current) {
        1f
    } else {
        rememberInfiniteTransition(label = "bolt").animateFloat(
            1f, BOLT_PULSE, infiniteRepeatable(tween(MotionTokens.DURATION_LONG_MS), RepeatMode.Reverse), label = "boltScale",
        ).value
    }
    Surface(color = colors.brandNavy, contentColor = colors.onBrandNavy, shape = MaterialTheme.shapes.large, shadowElevation = AmazgoneDimens.spaceSm, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = colors.cta, modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
            Column(Modifier.weight(1f)) {
                Text("${stringResource(Res.string.flash_banner_title)} ${hourOf(sale.windowStart)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(stringResource(Res.string.flash_banner_body), style = MaterialTheme.typography.bodySmall, color = colors.onBrandNavyVariant)
            }
            Surface(color = colors.onBrandNavy, contentColor = colors.brandNavy, shape = MaterialTheme.shapes.small) {
                Text(stringResource(Res.string.flash_banner_chip), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(AmazgoneDimens.spaceSm))
            }
        }
    }
}

private fun localTime(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

private fun clock(millis: Long): String = localTime(millis).let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }

private fun hourOf(millis: Long): Int = localTime(millis).hour
