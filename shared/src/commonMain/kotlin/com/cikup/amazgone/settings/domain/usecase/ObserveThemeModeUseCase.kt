package com.cikup.amazgone.settings.domain.usecase

import com.cikup.amazgone.settings.domain.model.ThemeMode
import com.cikup.amazgone.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveThemeModeUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settings.observeThemeMode().distinctUntilChanged()
}
