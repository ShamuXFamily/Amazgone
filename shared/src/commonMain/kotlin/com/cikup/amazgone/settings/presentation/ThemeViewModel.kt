package com.cikup.amazgone.settings.presentation

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.settings.domain.usecase.ObserveThemeModeUseCase
import com.cikup.amazgone.settings.domain.usecase.SetThemeModeUseCase

class ThemeViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val analytics: Analytics,
) : MviViewModel<ThemeState, ThemeIntent, ThemeEffect>(ThemeState()) {

    init {
        observeThemeMode().observe { setState { copy(mode = it, isLoaded = true) } }
    }

    override fun handleIntent(intent: ThemeIntent) {
        when (intent) {
            is ThemeIntent.Select -> {
                if (intent.mode != currentState.mode) analytics.themeChanged(intent.mode.name)
                setState { copy(mode = intent.mode) } // optimistic: the switch reacts instantly
                launchSafely { setThemeMode(intent.mode) }
            }
        }
    }
}
