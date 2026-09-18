package com.cikup.amazgone.account.data.remote

import com.cikup.amazgone.core.remote.FieldTransform
import com.cikup.amazgone.core.remote.FirestoreDocument
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.Precondition
import com.cikup.amazgone.core.remote.long
import com.cikup.amazgone.core.remote.longOrNull
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.wallet.domain.model.WalletRules

/** Firestore layout for user-owned documents; the single place that knows field names. */
object UserDocuments {
    fun user(uid: String) = "users/$uid"
    fun leaderboard(uid: String) = "leaderboard/$uid"
    fun order(uid: String, orderId: String) = "users/$uid/orders/$orderId"
    fun orders(uid: String) = "users/$uid/orders"
    fun reward(uid: String, rewardId: String) = "users/$uid/rewards/$rewardId"
    fun cartItem(uid: String, productId: String) = "users/$uid/cart/${encodeId(productId)}"
    fun cart(uid: String) = "users/$uid/cart"
    fun wishlistItem(uid: String, productId: String) = "users/$uid/wishlist/${encodeId(productId)}"
    fun wishlist(uid: String) = "users/$uid/wishlist"
    fun achievement(uid: String, id: String) = "users/$uid/achievements/$id"

    /** Product ids contain ':' which is fine in Firestore ids, but '/' would not be. */
    fun encodeId(productId: String) = productId.replace('/', '_')

    const val COINS = "coins"
    const val XP = "xp"
    const val LEVEL = "level"
    const val USERNAME = "username"
    const val ORDER_COUNT = "orderCount"
    const val LAST_SPIN_AT = "lastSpinAt"
    const val LAST_SCRATCH_AT = "lastScratchAt"
    const val LAST_DAILY_AT = "lastDailyAt"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val STARTER_GRANTED = "starterGranted"

    /** New account: starter coins, level 1, plus its public leaderboard row. Idempotent via exists=false. */
    fun createProfileWrites(uid: String, username: String): List<FirestoreWrite> = listOf(
        FirestoreWrite.Set(
            path = user(uid),
            fields = mapOf(
                USERNAME to username,
                COINS to WalletRules.STARTER_COINS,
                XP to 0L,
                LEVEL to 1L,
                ORDER_COUNT to 0L,
                STARTER_GRANTED to true,
            ),
            precondition = Precondition.Exists(false),
            transforms = listOf(FieldTransform.ServerTimestamp(CREATED_AT)),
        ),
        FirestoreWrite.Set(
            path = leaderboard(uid),
            fields = mapOf(USERNAME to username, XP to 0L, LEVEL to 1L),
            precondition = Precondition.Exists(false),
        ),
    )
}

/** Decoded users/{uid} document. */
data class RemoteUser(
    val username: String,
    val coins: Long,
    val xp: Long,
    val level: Long,
    val orderCount: Long,
    val lastSpinAt: Long?,
    val lastScratchAt: Long?,
    val lastDailyAt: Long?,
    val createdAt: Long,
)

fun FirestoreDocument.toRemoteUser() = RemoteUser(
    username = fields.string(UserDocuments.USERNAME).orEmpty(),
    coins = fields.long(UserDocuments.COINS),
    xp = fields.long(UserDocuments.XP),
    level = fields.long(UserDocuments.LEVEL).coerceAtLeast(1),
    orderCount = fields.long(UserDocuments.ORDER_COUNT),
    lastSpinAt = fields.longOrNull(UserDocuments.LAST_SPIN_AT),
    lastScratchAt = fields.longOrNull(UserDocuments.LAST_SCRATCH_AT),
    lastDailyAt = fields.longOrNull(UserDocuments.LAST_DAILY_AT),
    createdAt = fields.long(UserDocuments.CREATED_AT),
)
