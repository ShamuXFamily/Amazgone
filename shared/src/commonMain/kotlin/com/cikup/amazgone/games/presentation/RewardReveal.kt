package com.cikup.amazgone.games.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_plus_xp
import amazgone.shared.generated.resources.game_reward_saved
import amazgone.shared.generated.resources.game_you_won
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.games.domain.model.Reward
import org.jetbrains.compose.resources.stringResource

private const val START_SCALE = 0.3f

/** Prize card that springs up from small with an overshoot. */
@Composable
fun RewardReveal(reward: Reward, xp: Long, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember(reward) { Animatable(if (reduceMotion) 1f else START_SCALE) }
    LaunchedEffect(reward) { if (!reduceMotion) scale.animateTo(1f, MotionTokens.bouncy()) }
    ElevatedCard(modifier.fillMaxWidth().graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
        Column(
            Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Text(stringResource(Res.string.game_you_won), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rewardLabel(reward), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.game_plus_xp, Formatters.coins(xp)), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.game_reward_saved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
