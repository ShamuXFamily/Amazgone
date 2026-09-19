package com.cikup.amazgone.orders.data

import com.cikup.amazgone.account.data.remote.RemoteUser
import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.remote.toRemoteUser
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.Precondition
import com.cikup.amazgone.core.remote.TransactionResult
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.progress.domain.model.LevelCurve

const val REJECT_INSUFFICIENT_COINS = "INSUFFICIENT_COINS"
private const val FIELD_PAYLOAD = "payload"
private const val FIELD_DELIVERED_AT = "deliveredAt"

/**
 * Server-side checkout in one Firestore transaction: re-validates the balance, debits coins,
 * adds XP/level/orderCount, writes the order (exists=false ⇒ idempotent) and the leaderboard row.
 */
class OrderPlaceHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val orders: OrderRepositoryImpl,
) : OutboxHandler {
    override val type = OUTBOX_ORDER_PLACE

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(OrderPayload.serializer(), entry.payload)
        val firestore = firebase.requireFirestore()
        val outcome = try {
            firestore.runTransaction { tx ->
                // Retried blocks (after a conflict or a lost response) must not apply the order twice.
                if (tx.get(UserDocuments.order(session.uid, payload.orderId)) != null) {
                    return@runTransaction TransactionResult(emptyList(), PushResult.Success)
                }
                val user = tx.get(UserDocuments.user(session.uid))?.toRemoteUser()
                    ?: return@runTransaction TransactionResult(emptyList(), PushResult.Retry("profile not created yet"))
                if (user.coins < payload.totalCoins) {
                    return@runTransaction TransactionResult(emptyList(), PushResult.Rejected(REJECT_INSUFFICIENT_COINS))
                }
                TransactionResult(writes(session.uid, user, payload), PushResult.Success)
            }
        } catch (e: FirestoreException.PreconditionFailed) {
            PushResult.Success // order document already exists: an earlier attempt committed
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
        if (outcome == PushResult.Success) orders.markConfirmed(payload.orderId)
        return outcome
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        val payload = AppJson.decodeFromString(OrderPayload.serializer(), entry.payload)
        orders.markRejected(payload.orderId, reason)
    }

    private fun writes(uid: String, user: RemoteUser, payload: OrderPayload): List<FirestoreWrite> {
        val xp = user.xp + payload.xpEarned
        val level = LevelCurve.levelFor(xp).toLong()
        return listOf(
            FirestoreWrite.Set(
                path = UserDocuments.user(uid),
                fields = mapOf(
                    UserDocuments.COINS to user.coins - payload.totalCoins,
                    UserDocuments.XP to xp,
                    UserDocuments.LEVEL to level,
                    UserDocuments.ORDER_COUNT to user.orderCount + 1,
                ),
                mask = listOf(UserDocuments.COINS, UserDocuments.XP, UserDocuments.LEVEL, UserDocuments.ORDER_COUNT),
            ),
            FirestoreWrite.Set(
                path = UserDocuments.order(uid, payload.orderId),
                fields = mapOf(
                    "totalCoins" to payload.totalCoins,
                    "createdAt" to payload.createdAt,
                    FIELD_PAYLOAD to AppJson.encodeToString(OrderPayload.serializer(), payload),
                ),
                precondition = Precondition.Exists(false),
            ),
            FirestoreWrite.Set(
                path = UserDocuments.leaderboard(uid),
                fields = mapOf(UserDocuments.USERNAME to user.username, UserDocuments.XP to xp, UserDocuments.LEVEL to level),
            ),
        )
    }
}

/** Brings in orders placed on other devices. */
class OrdersPuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val orders: OrderRepositoryImpl,
) : RemotePuller {
    override val name = "orders"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val session = auth.session.value ?: return
        firebase.requireFirestore().list(UserDocuments.orders(session.uid)).forEach { doc ->
            val json = doc.fields.string(FIELD_PAYLOAD) ?: return@forEach
            runCatching { AppJson.decodeFromString(OrderPayload.serializer(), json) }
                .onSuccess { payload ->
                    orders.upsertRemote(payload)
                    (doc.fields[FIELD_DELIVERED_AT] as? Number)?.let { orders.applyRemoteDelivered(payload.orderId, it.toLong()) }
                }
        }
    }
}

/** Pushes "Order received"; firestore.rules only allow setting deliveredAt once on your own order. */
class OrderReceivedHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
) : OutboxHandler {
    override val type = OUTBOX_ORDER_RECEIVED

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(OrderReceivedPayload.serializer(), entry.payload)
        return try {
            firebase.requireFirestore().commit(
                listOf(
                    FirestoreWrite.Set(
                        path = UserDocuments.order(session.uid, payload.orderId),
                        fields = mapOf(FIELD_DELIVERED_AT to payload.deliveredAt),
                        mask = listOf(FIELD_DELIVERED_AT),
                        precondition = Precondition.Exists(true),
                    ),
                ),
            )
            PushResult.Success
        } catch (e: FirestoreException.PermissionDenied) {
            PushResult.Rejected(e.message ?: "denied") // already set on another device, or not our order
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
    }

    /** Nothing to undo: the local "received" mark is harmless even if the server already had one. */
    override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
}
