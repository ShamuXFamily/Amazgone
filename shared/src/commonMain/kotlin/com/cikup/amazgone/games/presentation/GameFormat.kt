package com.cikup.amazgone.games.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_reward_coins
import amazgone.shared.generated.resources.game_reward_coupon_flat
import amazgone.shared.generated.resources.game_reward_coupon_percent
import amazgone.shared.generated.resources.game_reward_xp
import androidx.compose.runtime.Composable
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.games.domain.model.Reward
import org.jetbrains.compose.resources.stringResource

private const val SECONDS = 1_000L
private const val MINUTE = 60L
private const val HOUR = 3_600L
private const val PAD = 2

/** 3_723_000 → "01:02:03". */
fun formatCountdown(millis: Long): String {
    val total = (millis + SECONDS - 1) / SECONDS
    val h = total / HOUR
    val m = (total % HOUR) / MINUTE
    val s = total % MINUTE
    return listOf(h, m, s).joinToString(":") { it.toString().padStart(PAD, '0') }
}

@Composable
fun rewardLabel(reward: Reward): String = when (reward) {
    is Reward.Coins -> stringResource(Res.string.game_reward_coins, Formatters.coins(reward.amount))
    is Reward.CouponReward -> when (reward.kind) {
        CouponKind.PERCENT -> stringResource(Res.string.game_reward_coupon_percent, reward.value.toInt())
        CouponKind.FLAT_COINS -> stringResource(Res.string.game_reward_coupon_flat, Formatters.coins(reward.value))
    }
    is Reward.XpBoost -> stringResource(Res.string.game_reward_xp, Formatters.coins(reward.xp))
}

/** Short text painted on wheel segments. */
fun rewardShortLabel(reward: Reward): String = when (reward) {
    is Reward.Coins -> Formatters.coins(reward.amount)
    is Reward.CouponReward -> if (reward.kind == CouponKind.PERCENT) "${reward.value}%" else "-${Formatters.coins(reward.value)}"
    is Reward.XpBoost -> "${reward.xp} XP"
}
