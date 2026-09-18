package com.cikup.amazgone.games.domain.repository

import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.model.GamePlay
import kotlinx.coroutines.flow.Flow

interface GamesRepository {
    fun observeLastPlayedAt(kind: GameKind): Flow<Long?>
    suspend fun lastPlayedAt(kind: GameKind): Long?
    /** Most recent play on this device (e.g. to show the last scratch card again). */
    fun observeLatestPlay(kind: GameKind): Flow<GamePlay?>
    /**
     * Atomically records the play and applies its reward locally (pending coins/XP or a coupon)
     * and queues the server confirmation.
     */
    suspend fun record(play: GamePlay)
}
