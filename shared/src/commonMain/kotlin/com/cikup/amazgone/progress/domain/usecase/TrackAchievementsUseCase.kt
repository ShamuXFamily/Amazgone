package com.cikup.amazgone.progress.domain.usecase

import com.cikup.amazgone.progress.domain.model.AchievementEngine
import com.cikup.amazgone.progress.domain.model.AchievementId
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach

/** Emits newly earned achievements (and persists them) whenever the stats change. */
class TrackAchievementsUseCase(private val achievements: AchievementRepository) {
    operator fun invoke(): Flow<Set<AchievementId>> =
        combine(achievements.observeStats(), achievements.observeUnlocked()) { stats, unlocked ->
            AchievementEngine.earned(stats) - unlocked.keys
        }
            .filter { it.isNotEmpty() }
            .onEach { achievements.unlock(it) }
}
