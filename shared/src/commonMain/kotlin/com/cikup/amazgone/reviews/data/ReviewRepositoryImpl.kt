package com.cikup.amazgone.reviews.data

import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreDocument
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.reviews.domain.model.ProductReview
import com.cikup.amazgone.reviews.domain.repository.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

const val OUTBOX_REVIEW_SAVE = "review.save"

@Serializable
data class ReviewPayload(val productId: String, val orderId: String, val rating: Int, val comment: String, val createdAt: Long)

/** Firestore layout: reviews/{productId}/entries/{uid} — public read, owner write (see firestore.rules). */
object ReviewDocuments {
    fun entries(productId: String) = "reviews/${UserDocuments.encodeId(productId)}/entries"
    fun entry(productId: String, uid: String) = "${entries(productId)}/$uid"
    const val USERNAME = "username"
    const val RATING = "rating"
    const val COMMENT = "comment"
    const val ORDER_ID = "orderId"
    const val CREATED_AT = "createdAt"
}

class ReviewRepositoryImpl(
    private val dao: ShopperReviewDao,
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val logger: AppLogger,
) : ReviewRepository, UserScopedStore {

    override fun observeForProduct(productId: String): Flow<List<ProductReview>> =
        dao.observeForProduct(productId).map { rows -> rows.map { it.toDomain() } }

    override fun observeMine(): Flow<Map<String, ProductReview>> =
        dao.observeMine().map { rows -> rows.associate { it.productId to it.toDomain() } }

    override suspend fun saveMine(productId: String, orderId: String, rating: Int, comment: String) = transactions.inTransaction {
        val payload = ReviewPayload(productId, orderId, rating, comment, time.nowMillis())
        val name = auth.session.value?.username ?: GUEST_NAME
        dao.upsert(listOf(ShopperReviewEntity(productId, ShopperReviewEntity.MINE, name, rating, comment, orderId, payload.createdAt, pending = true)))
        outbox.enqueue("review-$productId-${payload.createdAt}", OUTBOX_REVIEW_SAVE, AppJson.encodeToString(ReviewPayload.serializer(), payload))
    }

    override suspend fun refresh(productId: String) {
        val firestore = firebase.firestore ?: return
        val myUid = auth.session.value?.uid
        try {
            val docs = firestore.list(ReviewDocuments.entries(productId), authenticated = false)
            val mineLocal = dao.findMine(productId)
            val rows = docs.mapNotNull { doc ->
                val mine = doc.id == myUid
                if (mine && mineLocal?.pending == true) null else doc.toEntity(productId, mine)
            }
            dao.upsert(rows)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logger.error(TAG, "Refreshing reviews for $productId failed; showing cached ones", t)
        }
    }

    suspend fun markSynced(payload: ReviewPayload) = dao.markSynced(payload.productId, payload.createdAt)

    suspend fun dropRejected(payload: ReviewPayload) = dao.deleteMine(payload.productId, payload.createdAt)

    override suspend fun clearUserData() = dao.deleteAllMine()

    private companion object {
        const val TAG = "ReviewRepository"
        const val GUEST_NAME = "You"
    }
}

private fun FirestoreDocument.toEntity(productId: String, mine: Boolean): ShopperReviewEntity? {
    val rating = (fields[ReviewDocuments.RATING] as? Number)?.toInt()?.takeIf { it in 1..5 } ?: return null
    return ShopperReviewEntity(
        productId = productId,
        authorId = if (mine) ShopperReviewEntity.MINE else id,
        authorName = fields.string(ReviewDocuments.USERNAME) ?: return null,
        rating = rating,
        comment = fields.string(ReviewDocuments.COMMENT).orEmpty().take(com.cikup.amazgone.reviews.domain.model.ReviewRules.MAX_COMMENT),
        orderId = fields.string(ReviewDocuments.ORDER_ID).orEmpty(),
        createdAt = (fields[ReviewDocuments.CREATED_AT] as? Number)?.toLong() ?: 0L,
        pending = false,
    )
}

private fun ShopperReviewEntity.toDomain() = ProductReview(
    productId = productId,
    authorName = authorName,
    rating = rating,
    comment = comment,
    createdAt = createdAt,
    orderId = orderId,
    isMine = authorId == ShopperReviewEntity.MINE,
    isPending = pending,
)
