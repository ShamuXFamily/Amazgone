package com.cikup.amazgone.progress.domain.repository

import com.cikup.amazgone.progress.domain.model.AchievementId
import com.cikup.amazgone.progress.domain.model.LeaderboardEntry
import com.cikup.amazgone.progress.domain.model.PlayerStats
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeStats(): Flow<PlayerStats>
    fun observeUnlocked(): Flow<Map<AchievementId, Long>>
    /** Stores new unlocks (idempotent) and queues them for sync. */
    suspend fun unlock(ids: Set<AchievementId>)
}

interface LeaderboardRepository {
    fun observeEntries(): Flow<List<LeaderboardEntry>>
    fun observeSyncedAt(): Flow<Long?>
}
