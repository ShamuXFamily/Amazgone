package com.cikup.amazgone.delivery.data

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
import com.cikup.amazgone.delivery.domain.model.Courier

const val REJECT_COURIER_COINS = "INSUFFICIENT_COINS"

object CourierDocuments {
    fun couriers(uid: String) = "users/$uid/couriers"
    fun courier(uid: String, courier: String) = "${couriers(uid)}/$courier"
    const val PRICE = "price"
    const val PURCHASE_ID = "purchaseId"
    const val PURCHASED_AT = "purchasedAt"
}

/**
 * Server-side Garage purchase in one Firestore transaction: re-checks coins, debits the price and
 * creates users/{uid}/couriers/{COURIER} (rules check the price and the matching debit).
 */
class CourierBuyHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val couriers: CourierRepositoryImpl,
) : OutboxHandler {
    override val type = OUTBOX_COURIER_BUY

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(CourierPurchasePayload.serializer(), entry.payload)
        val docPath = CourierDocuments.courier(session.uid, payload.courier)
        val outcome = try {
            firebase.requireFirestore().runTransaction { tx ->
                if (tx.get(docPath) != null) return@runTransaction TransactionResult(emptyList(), PushResult.Success)
                val user = tx.get(UserDocuments.user(session.uid))?.toRemoteUser()
                    ?: return@runTransaction TransactionResult(emptyList(), PushResult.Retry("profile not created yet"))
                if (user.coins < payload.priceCoins) {
                    return@runTransaction TransactionResult(emptyList(), PushResult.Rejected(REJECT_COURIER_COINS))
                }
                val writes = listOf(
                    FirestoreWrite.Set(
                        path = UserDocuments.user(session.uid),
                        fields = mapOf(UserDocuments.COINS to user.coins - payload.priceCoins),
                        mask = listOf(UserDocuments.COINS),
                    ),
                    FirestoreWrite.Set(
                        path = docPath,
                        fields = mapOf(
                            CourierDocuments.PRICE to payload.priceCoins,
                            CourierDocuments.PURCHASE_ID to payload.purchaseId,
                            CourierDocuments.PURCHASED_AT to payload.purchasedAt,
                        ),
                        precondition = Precondition.Exists(false),
                    ),
                )
                TransactionResult(writes, PushResult.Success)
            }
        } catch (e: FirestoreException.PreconditionFailed) {
            PushResult.Success // an earlier attempt already committed
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
        if (outcome == PushResult.Success) couriers.markConfirmed(payload.purchaseId)
        return outcome
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        couriers.markRejected(AppJson.decodeFromString(CourierPurchasePayload.serializer(), entry.payload).purchaseId)
    }
}

/** Brings in couriers bought on other devices. */
class CouriersPuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val couriers: CourierRepositoryImpl,
) : RemotePuller {
    override val name = "couriers"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val session = auth.session.value ?: return
        firebase.requireFirestore().list(CourierDocuments.couriers(session.uid)).forEach { doc ->
            val courier = Courier.parse(doc.id) ?: return@forEach
            couriers.upsertRemote(
                courier,
                doc.fields.string(CourierDocuments.PURCHASE_ID) ?: doc.id,
                (doc.fields[CourierDocuments.PURCHASED_AT] as? Number)?.toLong() ?: 0L,
            )
        }
    }
}
