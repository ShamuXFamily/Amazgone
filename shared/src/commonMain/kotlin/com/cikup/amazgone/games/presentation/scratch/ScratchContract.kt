package com.cikup.amazgone.games.presentation.scratch

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.games.domain.usecase.PlayOutcome

data class ScratchState(
    val cooldown: Long = 0,
    /** The prize is decided (and recorded) when the card is handed out; scratching only reveals it. */
    val card: PlayOutcome? = null,
    val revealed: Boolean = false,
    val isLoading: Boolean = false,
) : UiState

sealed interface ScratchIntent : UiIntent {
    data object NewCard : ScratchIntent
    data object Revealed : ScratchIntent
    data object Back : ScratchIntent
}

sealed interface ScratchEffect : UiEffect {
    data object NavigateBack : ScratchEffect
}
