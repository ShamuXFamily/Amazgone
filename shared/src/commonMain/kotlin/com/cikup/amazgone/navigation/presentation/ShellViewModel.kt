package com.cikup.amazgone.navigation.presentation

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.account.domain.usecase.ObserveSessionUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartCountUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import com.cikup.amazgone.progress.domain.usecase.TrackAchievementsUseCase

class ShellViewModel(
    observeCartCount: ObserveCartCountUseCase,
    observeProgress: ObserveProgressUseCase,
    trackAchievements: TrackAchievementsUseCase,
    observeSession: ObserveSessionUseCase,
    private val analytics: Analytics,
) : MviViewModel<ShellState, ShellIntent, ShellEffect>(ShellState()) {

    private var lastLevel: Int? = null

    init {
        observeCartCount().observe { setState { copy(cartCount = it) } }
        trackAchievements().observe { unlocked ->
            unlocked.forEach { analytics.unlockAchievement(it.name) }
            sendEffect(ShellEffect.AchievementsUnlocked(unlocked))
        }
        observeSession().observe { analytics.identify(it?.uid) }
        observeProgress().observe { progress ->
            val previous = lastLevel
            lastLevel = progress.level
            // the first emission is the starting level, not a level-up
            if (previous != null && progress.level > previous) {
                analytics.levelUp(progress.level)
                setState { copy(levelUpTo = progress.level) }
            }
        }
    }

    override fun handleIntent(intent: ShellIntent) = when (intent) {
        ShellIntent.DismissLevelUp -> setState { copy(levelUpTo = null) }
    }
}
