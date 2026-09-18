package com.cikup.amazgone.progress.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.level_short
import amazgone.shared.generated.resources.xp_to_next
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.progress.domain.model.LevelCurve
import org.jetbrains.compose.resources.stringResource

/** "Lv 3" chip with an XP bar that springs to its new value when XP changes. */
@Composable
fun LevelBadge(xp: Long, modifier: Modifier = Modifier) {
    val level = LevelCurve.levelFor(xp)
    val progress by animateFloatAsState(LevelCurve.progress(xp), MotionTokens.bouncy(), label = "xpBar")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    stringResource(Res.string.level_short, level),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2),
                )
            }
            Text(
                stringResource(Res.string.xp_to_next, Formatters.coins(xp), Formatters.coins(LevelCurve.xpForLevel(level + 1))),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(AmazgoneDimens.productCardMinWidth))
    }
}
