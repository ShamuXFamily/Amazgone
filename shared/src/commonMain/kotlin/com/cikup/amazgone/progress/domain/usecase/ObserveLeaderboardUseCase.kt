package com.cikup.amazgone.progress.domain.usecase

import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.progress.domain.model.Leaderboard
import com.cikup.amazgone.progress.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveLeaderboardUseCase(private val leaderboard: LeaderboardRepository, private val auth: AuthRepository) {
    operator fun invoke(): Flow<Leaderboard> = combine(
        leaderboard.observeEntries(),
        leaderboard.observeSyncedAt(),
        auth.session,
    ) { entries, syncedAt, session -> Leaderboard(entries, syncedAt, session?.uid) }
}
