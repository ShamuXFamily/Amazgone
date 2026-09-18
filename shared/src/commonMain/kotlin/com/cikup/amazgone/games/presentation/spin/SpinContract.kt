package com.cikup.amazgone.games.presentation.spin

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.games.domain.model.RewardTables

data class SpinState(
    val segments: List<Reward> = RewardTables.SPIN,
    val cooldown: Long = 0,
    /** Set when a spin starts; the UI animates onto it, then sends [SpinIntent.Landed]. */
    val targetIndex: Int? = null,
    val spinId: Int = 0,
    val isSpinning: Boolean = false,
    val wonReward: Reward? = null,
    val wonXp: Long = 0,
) : UiState {
    val canSpin: Boolean get() = cooldown <= 0 && !isSpinning
}

sealed interface SpinIntent : UiIntent {
    data object Spin : SpinIntent
    data object Landed : SpinIntent
    data object Back : SpinIntent
}

sealed interface SpinEffect : UiEffect {
    data object NavigateBack : SpinEffect
}
