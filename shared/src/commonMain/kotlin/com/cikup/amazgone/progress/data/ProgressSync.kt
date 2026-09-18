package com.cikup.amazgone.progress.data

import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.remote.toRemoteUser
import com.cikup.amazgone.account.data.sync.pushResultOf
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FieldTransform
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.Precondition
import com.cikup.amazgone.core.remote.TransactionResult
import com.cikup.amazgone.core.remote.long
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.progress.domain.model.LevelCurve
import com.cikup.amazgone.progress.domain.model.XpRules
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import kotlinx.serialization.Serializable

private const val LEADERBOARD_SIZE = 50
private const val DAY_MS = 24 * 60 * 60 * 1_000L
/** Minimum spacing between daily bonuses (a little under a day to tolerate time zones / clock skew). */
private const val DAILY_MIN_GAP_MS = 20 * 60 * 60 * 1_000L
const val OUTBOX_DAILY_XP = "xp.daily"
const val REJECT_ALREADY_CLAIMED = "ALREADY_CLAIMED_TODAY"

class LeaderboardPuller(
    private val firebase: FirebaseServices,
    private val repository: LeaderboardRepositoryImpl,
    private val time: TimeProvider,
) : RemotePuller {
    override val name = "leaderboard"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val docs = firebase.requireFirestore().query("leaderboard", orderBy = UserDocuments.XP, descending = true, limit = LEADERBOARD_SIZE)
        val entries = docs.mapIndexed { index, doc ->
            LeaderboardEntity(
                uid = doc.id,
                username = doc.fields.string(UserDocuments.USERNAME).orEmpty(),
                xp = doc.fields.long(UserDocuments.XP),
                level = doc.fields.long(UserDocuments.LEVEL).toInt().coerceAtLeast(1),
                rank = index + 1,
            )
        }
        repository.replace(entries, time.nowMillis())
    }
}

class AchievementUnlockHandler(private val auth: AuthRepository, private val firebase: FirebaseServices) : OutboxHandler {
    override val type = OUTBOX_ACHIEVEMENT_UNLOCK

    override suspend fun push(entry: OutboxEntry): PushResult {
        val uid = auth.session.value?.uid ?: return PushResult.Retry("signed out")
        return pushResultOf {
            firebase.requireFirestore().commit(
                listOf(
                    FirestoreWrite.Set(UserDocuments.achievement(uid, entry.payload), mapOf("unlockedAt" to entry.createdAt), precondition = Precondition.Exists(false)),
                ),
            )
        }
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
}

class AchievementsPuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val repository: AchievementRepositoryImpl,
) : RemotePuller {
    override val name = "achievements"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val uid = auth.session.value?.uid ?: return
        firebase.requireFirestore().list("users/$uid/achievements").forEach { doc ->
            repository.restore(doc.id, doc.fields.long("unlockedAt"))
        }
    }
}

@Serializable
data class DailyXpPayload(val id: String, val claimedAt: Long)

/** Once per calendar day on launch: +20 XP locally (pending) and a queued server claim. */
class DailyVisitTask(
    private val wallet: WalletRepositoryImpl,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : StartupTask {
    override val name = "daily-visit"

    override suspend fun run() {
        val now = time.nowMillis()
        val last = wallet.lastEntryAt(LedgerReason.DAILY_XP)
        if (last != null && last / DAY_MS == now / DAY_MS) return
        val id = ids.newId()
        transactions.inTransaction {
            wallet.recordPending(Currency.XP, XpRules.DAILY_LOGIN_XP, LedgerReason.DAILY_XP, id)
            outbox.enqueue(id, OUTBOX_DAILY_XP, AppJson.encodeToString(DailyXpPayload.serializer(), DailyXpPayload(id, now)))
        }
    }
}

class DailyXpHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val wallet: WalletRepositoryImpl,
) : OutboxHandler {
    override val type = OUTBOX_DAILY_XP

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(DailyXpPayload.serializer(), entry.payload)
        val firestore = firebase.requireFirestore()
        val outcome = try {
            firestore.runTransaction { tx ->
                val marker = UserDocuments.reward(session.uid, "daily-${payload.id}")
                if (tx.get(marker) != null) return@runTransaction TransactionResult(emptyList(), PushResult.Success) // already applied
                val user = tx.get(UserDocuments.user(session.uid))?.toRemoteUser()
                    ?: return@runTransaction TransactionResult(emptyList(), PushResult.Retry("profile not created yet"))
                val last = user.lastDailyAt
                if (last != null && payload.claimedAt - last < DAILY_MIN_GAP_MS) {
                    return@runTransaction TransactionResult(emptyList(), PushResult.Rejected(REJECT_ALREADY_CLAIMED))
                }
                val xp = user.xp + XpRules.DAILY_LOGIN_XP
                val level = LevelCurve.levelFor(xp).toLong()
                TransactionResult(
                    listOf(
                        FirestoreWrite.Set(
                            UserDocuments.user(session.uid),
                            mapOf(UserDocuments.XP to xp, UserDocuments.LEVEL to level),
                            mask = listOf(UserDocuments.XP, UserDocuments.LEVEL),
                            transforms = listOf(FieldTransform.ServerTimestamp(UserDocuments.LAST_DAILY_AT)),
                        ),
                        FirestoreWrite.Set(UserDocuments.leaderboard(session.uid), mapOf(UserDocuments.USERNAME to user.username, UserDocuments.XP to xp, UserDocuments.LEVEL to level)),
                        FirestoreWrite.Set(marker, mapOf("kind" to "DAILY", "coins" to 0L, "xp" to XpRules.DAILY_LOGIN_XP)),
                    ),
                    PushResult.Success,
                )
            }
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
        if (outcome == PushResult.Success) wallet.confirm(payload.id)
        return outcome
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        wallet.reject(AppJson.decodeFromString(DailyXpPayload.serializer(), entry.payload).id)
    }
}
