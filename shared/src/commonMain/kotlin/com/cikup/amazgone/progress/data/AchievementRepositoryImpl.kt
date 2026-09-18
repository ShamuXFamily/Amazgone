package com.cikup.amazgone.progress.data

import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.games.data.GamePlayDao
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.orders.data.OrderDao
import com.cikup.amazgone.progress.domain.model.AchievementId
import com.cikup.amazgone.progress.domain.model.LevelCurve
import com.cikup.amazgone.progress.domain.model.PlayerStats
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import com.cikup.amazgone.wishlist.data.WishlistDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

const val OUTBOX_ACHIEVEMENT_UNLOCK = "achievement.unlock"

class AchievementRepositoryImpl(
    private val dao: AchievementDao,
    private val orders: OrderDao,
    private val plays: GamePlayDao,
    private val wishlist: WishlistDao,
    private val wallet: WalletRepository,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
) : AchievementRepository, UserScopedStore {

    override fun observeStats(): Flow<PlayerStats> = combine(
        orders.observeActiveCount(),
        orders.observeSpent(),
        plays.observeCount(GameKind.SPIN.name),
        plays.observeCount(GameKind.SCRATCH.name),
        combine(wallet.observeBalance(Currency.XP), wishlist.observeCount()) { xp, saved -> xp to saved },
    ) { orderCount, spent, spins, scratches, (xp, saved) ->
        PlayerStats(orderCount, spent, spins, scratches, LevelCurve.levelFor(xp), saved)
    }.distinctUntilChanged()

    override fun observeUnlocked(): Flow<Map<AchievementId, Long>> = dao.observeAll().map { rows ->
        rows.mapNotNull { row -> AchievementId.entries.firstOrNull { it.name == row.id }?.let { it to row.unlockedAt } }.toMap()
    }

    /** The achievement id doubles as the outbox id, so each unlock syncs exactly once. */
    override suspend fun unlock(ids: Set<AchievementId>) = transactions.inTransaction {
        val now = time.nowMillis()
        ids.forEach { id ->
            if (dao.insert(AchievementEntity(id.name, now)) != -1L) outbox.enqueue("ach-${id.name}", OUTBOX_ACHIEVEMENT_UNLOCK, id.name)
        }
    }

    suspend fun restore(id: String, unlockedAt: Long) {
        dao.insert(AchievementEntity(id, unlockedAt))
    }

    override suspend fun clearUserData() = dao.deleteAll()
}
