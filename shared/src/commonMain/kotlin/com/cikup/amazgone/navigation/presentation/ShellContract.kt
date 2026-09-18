package com.cikup.amazgone.navigation.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.progress.domain.model.AchievementId

/** App-wide chrome state: nav badges and celebration overlays. */
data class ShellState(val cartCount: Int = 0, val levelUpTo: Int? = null) : UiState

sealed interface ShellIntent : UiIntent {
    data object DismissLevelUp : ShellIntent
}

sealed interface ShellEffect : UiEffect {
    data class AchievementsUnlocked(val ids: Set<AchievementId>) : ShellEffect
}
