package com.cikup.amazgone.progress.domain.usecase

import com.cikup.amazgone.progress.domain.model.Achievement
import com.cikup.amazgone.progress.domain.model.AchievementEngine
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveAchievementsUseCase(private val achievements: AchievementRepository) {
    operator fun invoke(): Flow<List<Achievement>> =
        combine(achievements.observeStats(), achievements.observeUnlocked()) { stats, unlocked -> AchievementEngine.board(stats, unlocked) }
}
