package com.cikup.amazgone.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens

private const val MILLIS = 1_000L
private const val MINUTE = 60L
private const val HOUR = 3_600L
private const val PAD = 2

/** "01 · 17 · 12" style countdown; each digit rolls independently (odometer) when it changes. */
@Composable
fun CountdownBoxes(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    separator: String? = null,
) {
    val total = (remainingMillis + MILLIS - 1) / MILLIS
    val parts = listOf(total / HOUR, (total % HOUR) / MINUTE, total % MINUTE).map { it.toString().padStart(PAD, '0') }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { index, part ->
            if (index > 0 && separator != null) Text(separator, style = style, color = container)
            Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
                Row(Modifier.padding(horizontal = AmazgoneDimens.spaceXs, vertical = AmazgoneDimens.spaceXs / 2)) {
                    part.forEach { digit -> RollingDigit(digit, style) }
                }
            }
        }
    }
}

@Composable
fun RollingDigit(digit: Char, style: TextStyle, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = digit,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically(MotionTokens.snappy()) { -it } + fadeIn())
                .togetherWith(slideOutVertically(MotionTokens.snappy()) { it } + fadeOut())
        },
        label = "digit",
    ) { Text(it.toString(), style = style) }
}
