package com.cikup.amazgone.progress.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.progress.domain.usecase.ObserveAchievementsUseCase

class AchievementsViewModel(observeAchievements: ObserveAchievementsUseCase) :
    MviViewModel<AchievementsState, AchievementsIntent, AchievementsEffect>(AchievementsState()) {
    init {
        observeAchievements().observe { setState { copy(achievements = it) } }
    }

    override fun handleIntent(intent: AchievementsIntent) = when (intent) {
        AchievementsIntent.Back -> sendEffect(AchievementsEffect.NavigateBack)
    }
}
