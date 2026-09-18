package com.cikup.amazgone.settings.domain.repository

import com.cikup.amazgone.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device-level preferences. They are local only and survive sign-out. */
interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
