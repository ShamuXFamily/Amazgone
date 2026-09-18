package com.cikup.amazgone.games.data

import com.cikup.amazgone.account.data.remote.RemoteUser
import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.remote.toRemoteUser
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FieldTransform
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.Precondition
import com.cikup.amazgone.core.remote.TransactionResult
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.games.domain.model.Cooldowns
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.progress.domain.model.LevelCurve
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl

const val REJECT_COOLDOWN = "COOLDOWN"

/** Tolerates small device/server clock differences when re-checking the cooldown. */
private const val CLOCK_SKEW_TOLERANCE_MS = 5 * 60 * 1_000L

/**
 * Applies a game reward on the server in one transaction: re-checks the cooldown against the
 * server's last play (set with a server timestamp), credits coins/XP and writes an idempotent
 * reward document. The security rules enforce the same cooldown against request.time.
 */
class GameRewardHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val wallet: WalletRepositoryImpl,
) : OutboxHandler {
    override val type = OUTBOX_GAME_REWARD

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(GameRewardPayload.serializer(), entry.payload)
        val kind = GameKind.valueOf(payload.kind)
        val firestore = firebase.requireFirestore()
        val outcome = try {
            firestore.runTransaction { tx ->
                val user = firestore.get(UserDocuments.user(session.uid), tx)?.toRemoteUser()
                    ?: return@runTransaction TransactionResult(emptyList(), PushResult.Retry("profile not created yet"))
                val lastServerPlay = if (kind == GameKind.SPIN) user.lastSpinAt else user.lastScratchAt
                if (Cooldowns.remaining(kind, lastServerPlay, payload.playedAt + CLOCK_SKEW_TOLERANCE_MS) > 0) {
                    return@runTransaction TransactionResult(emptyList(), PushResult.Rejected(REJECT_COOLDOWN))
                }
                TransactionResult(writes(session.uid, user, payload, kind), PushResult.Success)
            }
        } catch (e: FirestoreException.PreconditionFailed) {
            PushResult.Success // reward document exists: already applied
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
        if (outcome == PushResult.Success) wallet.confirm(payload.playId)
        return outcome
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        val payload = AppJson.decodeFromString(GameRewardPayload.serializer(), entry.payload)
        wallet.reject(payload.playId)
    }

    private fun writes(uid: String, user: RemoteUser, payload: GameRewardPayload, kind: GameKind): List<FirestoreWrite> {
        val xp = user.xp + payload.xp
        val level = LevelCurve.levelFor(xp).toLong()
        val playedField = if (kind == GameKind.SPIN) UserDocuments.LAST_SPIN_AT else UserDocuments.LAST_SCRATCH_AT
        return listOf(
            FirestoreWrite.Set(
                UserDocuments.user(uid),
                mapOf(UserDocuments.COINS to user.coins + payload.coins, UserDocuments.XP to xp, UserDocuments.LEVEL to level),
                mask = listOf(UserDocuments.COINS, UserDocuments.XP, UserDocuments.LEVEL),
                transforms = listOf(FieldTransform.ServerTimestamp(playedField)),
            ),
            FirestoreWrite.Set(
                UserDocuments.reward(uid, payload.playId),
                mapOf("kind" to payload.kind, "coins" to payload.coins, "xp" to payload.xp),
                precondition = Precondition.Exists(false),
            ),
            FirestoreWrite.Set(
                UserDocuments.leaderboard(uid),
                mapOf(UserDocuments.USERNAME to user.username, UserDocuments.XP to xp, UserDocuments.LEVEL to level),
            ),
        )
    }
}
