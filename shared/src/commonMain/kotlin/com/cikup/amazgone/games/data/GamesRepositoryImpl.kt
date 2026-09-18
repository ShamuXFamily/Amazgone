package com.cikup.amazgone.games.data

import com.cikup.amazgone.account.data.local.UserProfileDao
import com.cikup.amazgone.account.data.local.UserProfileEntity
import com.cikup.amazgone.cart.data.coupon.CouponRepositoryImpl
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.model.GamePlay
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.games.domain.repository.GamesRepository
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

const val OUTBOX_GAME_REWARD = "game.reward"
private const val COUPON_VALIDITY_MS = 7 * 24 * 60 * 60 * 1_000L

@Serializable
data class GameRewardPayload(val playId: String, val kind: String, val coins: Long, val xp: Long, val playedAt: Long)

class GamesRepositoryImpl(
    private val dao: GamePlayDao,
    private val profiles: UserProfileDao,
    private val wallet: WalletRepositoryImpl,
    private val coupons: CouponRepositoryImpl,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
) : GamesRepository, UserScopedStore {

    /** Latest of this device's plays and the server's record (so reinstalling doesn't reset cooldowns). */
    override fun observeLastPlayedAt(kind: GameKind): Flow<Long?> =
        combine(dao.observeLastPlayedAt(kind.name), profiles.observe()) { local, profile ->
            listOfNotNull(local, profile?.serverLastPlayed(kind)).maxOrNull()
        }

    override suspend fun lastPlayedAt(kind: GameKind): Long? =
        listOfNotNull(dao.lastPlayedAt(kind.name), profiles.current()?.serverLastPlayed(kind)).maxOrNull()

    override fun observeLatestPlay(kind: GameKind): Flow<GamePlay?> = dao.observeLatest(kind.name).map { row ->
        row?.let { GamePlay(it.id, GameKind.valueOf(it.kind), it.rewardIndex, it.xp, it.playedAt) }
    }

    override suspend fun record(play: GamePlay) = transactions.inTransaction {
        val reward = play.reward
        val coins = (reward as? Reward.Coins)?.amount ?: 0L
        dao.insert(GamePlayEntity(play.id, play.kind.name, play.rewardIndex, coins, play.xp, play.playedAt))
        if (coins > 0) wallet.recordPending(Currency.COINS, coins, rewardReason(play.kind), play.id)
        wallet.recordPending(Currency.XP, play.xp, LedgerReason.GAME_XP, play.id)
        if (reward is Reward.CouponReward) {
            coupons.grant(
                Coupon(
                    code = "${play.kind.name}-${play.id.take(COUPON_CODE_LENGTH).uppercase()}",
                    kind = reward.kind,
                    value = reward.value,
                    minSubtotalCoins = reward.minSubtotalCoins,
                    expiresAt = play.playedAt + COUPON_VALIDITY_MS,
                ),
            )
        }
        outbox.enqueue(
            play.id,
            OUTBOX_GAME_REWARD,
            AppJson.encodeToString(GameRewardPayload.serializer(), GameRewardPayload(play.id, play.kind.name, coins, play.xp, play.playedAt)),
        )
    }

    override suspend fun clearUserData() = dao.deleteAll()

    private fun rewardReason(kind: GameKind) = when (kind) {
        GameKind.SPIN -> LedgerReason.SPIN_REWARD
        GameKind.SCRATCH -> LedgerReason.SCRATCH_REWARD
    }

    private companion object {
        const val COUPON_CODE_LENGTH = 6
    }
}

private fun UserProfileEntity.serverLastPlayed(kind: GameKind): Long? = when (kind) {
    GameKind.SPIN -> lastSpinAt
    GameKind.SCRATCH -> lastScratchAt
}
