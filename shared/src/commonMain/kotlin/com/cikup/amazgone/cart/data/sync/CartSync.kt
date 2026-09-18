package com.cikup.amazgone.cart.data.sync

import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.sync.pushResultOf
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.cart.data.repository.CartRepositoryImpl
import com.cikup.amazgone.cart.data.repository.CartSetPayload
import com.cikup.amazgone.cart.data.repository.OUTBOX_CART_SET
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.long
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller

private const val FIELD_PRODUCT = "productId"
private const val FIELD_QUANTITY = "quantity"
private const val FIELD_UPDATED = "updatedAt"

/** Mirrors each cart line to users/{uid}/cart (quantity 0 kept as a tombstone). */
class CartSetHandler(private val auth: AuthRepository, private val firebase: FirebaseServices) : OutboxHandler {
    override val type = OUTBOX_CART_SET

    override suspend fun push(entry: OutboxEntry): PushResult {
        val uid = auth.session.value?.uid ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(CartSetPayload.serializer(), entry.payload)
        return pushResultOf {
            firebase.requireFirestore().commit(
                listOf(
                    FirestoreWrite.Set(
                        UserDocuments.cartItem(uid, payload.productId),
                        mapOf(FIELD_PRODUCT to payload.productId, FIELD_QUANTITY to payload.quantity.toLong(), FIELD_UPDATED to payload.updatedAt),
                    ),
                ),
            )
        }
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
}

class CartPuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val cart: CartRepositoryImpl,
) : RemotePuller {
    override val name = "cart"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val uid = auth.session.value?.uid ?: return
        firebase.requireFirestore().list(UserDocuments.cart(uid)).forEach { doc ->
            val productId = doc.fields.string(FIELD_PRODUCT) ?: return@forEach
            cart.mergeRemote(productId, doc.fields.long(FIELD_QUANTITY).toInt(), doc.fields.long(FIELD_UPDATED))
        }
    }
}
