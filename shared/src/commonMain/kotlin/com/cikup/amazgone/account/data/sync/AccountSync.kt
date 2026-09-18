package com.cikup.amazgone.account.data.sync

import com.cikup.amazgone.account.data.local.UserProfileEntity
import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.remote.toRemoteUser
import com.cikup.amazgone.account.data.repository.OUTBOX_PROFILE_CREATE
import com.cikup.amazgone.account.data.repository.ProfileCreatePayload
import com.cikup.amazgone.account.data.repository.ProfileRepositoryImpl
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.core.sync.domain.SyncGate
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl

/** Pushing requires a signed-in account and a configured Firebase project. */
class SessionSyncGate(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
) : SyncGate {
    override suspend fun canPush(): Boolean = auth.session.value != null && firebase.isAvailable
}

/** Maps Firestore failures to outbox outcomes shared by every handler. */
suspend fun pushResultOf(block: suspend () -> Unit): PushResult = try {
    block()
    PushResult.Success
} catch (e: FirestoreException.PreconditionFailed) {
    // exists=false precondition on an idempotency doc: this mutation was already applied.
    PushResult.Success
} catch (e: FirestoreException) {
    PushResult.Retry(e.message ?: e::class.simpleName.orEmpty())
}

class ProfileCreateHandler(private val firebase: FirebaseServices) : OutboxHandler {
    override val type = OUTBOX_PROFILE_CREATE

    override suspend fun push(entry: OutboxEntry): PushResult {
        val payload = AppJson.decodeFromString(ProfileCreatePayload.serializer(), entry.payload)
        return pushResultOf { firebase.requireFirestore().commit(UserDocuments.createProfileWrites(payload.uid, payload.username)) }
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
}

/**
 * Pulls users/{uid}: caches the profile and re-bases the wallet/XP ledger on server totals.
 * Self-healing: if the account has no server profile (e.g. the registration's queued creation was
 * lost), it is created here directly — otherwise every queued order/reward would wait on it forever.
 */
class ProfilePuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val profiles: ProfileRepositoryImpl,
    private val wallet: WalletRepositoryImpl,
    private val time: TimeProvider,
) : RemotePuller {
    override val name = "profile"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val session = auth.session.value ?: return
        val firestore = firebase.requireFirestore()
        val document = firestore.get(UserDocuments.user(session.uid)) ?: run {
            try {
                firestore.commit(UserDocuments.createProfileWrites(session.uid, session.username))
            } catch (_: FirestoreException.PreconditionFailed) {
                // created concurrently (e.g. by the queued profile.create) — fine, read it below
            }
            firestore.get(UserDocuments.user(session.uid))
        } ?: return
        val user = document.toRemoteUser()
        profiles.save(
            UserProfileEntity(
                uid = session.uid,
                username = user.username.ifBlank { session.username },
                orderCount = user.orderCount.toInt(),
                lastSpinAt = user.lastSpinAt,
                lastScratchAt = user.lastScratchAt,
                createdAt = user.createdAt,
                syncedAt = time.nowMillis(),
            ),
        )
        wallet.applyServerBaseline(coins = user.coins, xp = user.xp)
    }
}
