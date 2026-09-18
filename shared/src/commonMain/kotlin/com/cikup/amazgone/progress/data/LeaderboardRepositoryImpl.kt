package com.cikup.amazgone.progress.data

import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.data.SyncMetaDao
import com.cikup.amazgone.core.sync.data.SyncMetaEntity
import com.cikup.amazgone.progress.domain.model.LeaderboardEntry
import com.cikup.amazgone.progress.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LeaderboardRepositoryImpl(
    private val dao: LeaderboardDao,
    private val syncMeta: SyncMetaDao,
    private val transactions: TransactionRunner,
) : LeaderboardRepository, UserScopedStore {

    override fun observeEntries(): Flow<List<LeaderboardEntry>> =
        dao.observeAll().map { rows -> rows.map { LeaderboardEntry(it.uid, it.username, it.xp, it.level, it.rank) } }

    override fun observeSyncedAt(): Flow<Long?> = syncMeta.observeLastSyncedAt(META_KEY)

    suspend fun replace(entries: List<LeaderboardEntity>, now: Long) = transactions.inTransaction {
        dao.deleteAll()
        dao.insertAll(entries)
        syncMeta.upsert(SyncMetaEntity(META_KEY, now))
    }

    override suspend fun clearUserData() = dao.deleteAll()

    private companion object {
        const val META_KEY = "leaderboard"
    }
}
