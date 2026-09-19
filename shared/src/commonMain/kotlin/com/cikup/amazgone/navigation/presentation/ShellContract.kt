package com.cikup.amazgone.navigation.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.progress.domain.model.AchievementId

/** App-wide chrome state: nav badges, celebration overlays and the in-app notification banner. */
data class ShellState(
    val cartCount: Int = 0,
    val levelUpTo: Int? = null,
    /** A notification that just arrived while the app is open; slides down from the top. */
    val banner: AppNotification? = null,
) : UiState

sealed interface ShellIntent : UiIntent {
    data object DismissLevelUp : ShellIntent
    data object DismissBanner : ShellIntent
    data object OpenBanner : ShellIntent
}

sealed interface ShellEffect : UiEffect {
    data class AchievementsUnlocked(val ids: Set<AchievementId>) : ShellEffect
    data class OpenLink(val link: String) : ShellEffect
}
