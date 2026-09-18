package com.cikup.amazgone.progress.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.progress.domain.model.Achievement

data class AchievementsState(val achievements: List<Achievement> = emptyList()) : UiState {
    val unlockedCount: Int get() = achievements.count { it.isUnlocked }
}

sealed interface AchievementsIntent : UiIntent {
    data object Back : AchievementsIntent
}

sealed interface AchievementsEffect : UiEffect {
    data object NavigateBack : AchievementsEffect
}
