package com.cikup.amazgone.progress.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.core.sync.domain.SyncStatus
import com.cikup.amazgone.progress.domain.model.Leaderboard

data class LeaderboardState(
    val leaderboard: Leaderboard = Leaderboard(emptyList(), null, null),
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val isRefreshing: Boolean = false,
) : UiState

sealed interface LeaderboardIntent : UiIntent {
    data object Refresh : LeaderboardIntent
    data object Back : LeaderboardIntent
}

sealed interface LeaderboardEffect : UiEffect {
    data object NavigateBack : LeaderboardEffect
}
