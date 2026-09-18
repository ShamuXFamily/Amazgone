package com.cikup.amazgone.progress.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.core.sync.domain.usecase.ObserveIsSyncingUseCase
import com.cikup.amazgone.core.sync.domain.usecase.ObserveSyncStatusUseCase
import com.cikup.amazgone.core.sync.domain.usecase.RequestSyncUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveLeaderboardUseCase

class LeaderboardViewModel(
    observeLeaderboard: ObserveLeaderboardUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    observeIsSyncing: ObserveIsSyncingUseCase,
    private val requestSync: RequestSyncUseCase,
) : MviViewModel<LeaderboardState, LeaderboardIntent, LeaderboardEffect>(LeaderboardState()) {
    init {
        observeLeaderboard().observe { setState { copy(leaderboard = it) } }
        observeSyncStatus().observe { setState { copy(syncStatus = it) } }
        observeIsSyncing().observe { setState { copy(isRefreshing = it) } }
        requestSync(force = true)
    }

    override fun handleIntent(intent: LeaderboardIntent) = when (intent) {
        LeaderboardIntent.Refresh -> requestSync(force = true)
        LeaderboardIntent.Back -> sendEffect(LeaderboardEffect.NavigateBack)
    }
}
