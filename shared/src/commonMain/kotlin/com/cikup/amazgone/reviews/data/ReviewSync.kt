package com.cikup.amazgone.reviews.data

import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.PushResult

/** Publishes the user's review under their uid; editing overwrites the same document. */
class ReviewSaveHandler(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val reviews: ReviewRepositoryImpl,
) : OutboxHandler {
    override val type = OUTBOX_REVIEW_SAVE

    override suspend fun push(entry: OutboxEntry): PushResult {
        val session = auth.session.value ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(ReviewPayload.serializer(), entry.payload)
        return try {
            firebase.requireFirestore().commit(
                listOf(
                    FirestoreWrite.Set(
                        path = ReviewDocuments.entry(payload.productId, session.uid),
                        fields = mapOf(
                            ReviewDocuments.USERNAME to session.username,
                            ReviewDocuments.RATING to payload.rating.toLong(),
                            ReviewDocuments.COMMENT to payload.comment,
                            ReviewDocuments.ORDER_ID to payload.orderId,
                            ReviewDocuments.CREATED_AT to payload.createdAt,
                        ),
                    ),
                ),
            )
            reviews.markSynced(payload)
            PushResult.Success
        } catch (e: FirestoreException.PermissionDenied) {
            PushResult.Rejected(e.message ?: "denied")
        } catch (e: FirestoreException) {
            PushResult.Retry(e.message ?: "firestore error")
        }
    }

    /** The server refused it (e.g. the order isn't on this account): remove the local copy. */
    override suspend fun onRejected(entry: OutboxEntry, reason: String) {
        reviews.dropRejected(AppJson.decodeFromString(ReviewPayload.serializer(), entry.payload))
    }
}
