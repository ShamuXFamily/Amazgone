package com.cikup.amazgone.reviews.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Shopper reviews cached locally. The current user's own review always uses [MINE] as author id. */
@Entity(tableName = "shopper_reviews", primaryKeys = ["productId", "authorId"], indices = [Index("authorId")])
data class ShopperReviewEntity(
    val productId: String,
    val authorId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val orderId: String,
    val createdAt: Long,
    val pending: Boolean,
) {
    companion object {
        const val MINE = "me"
    }
}

@Dao
interface ShopperReviewDao {
    @Query("SELECT * FROM shopper_reviews WHERE productId = :productId ORDER BY (authorId = 'me') DESC, createdAt DESC")
    fun observeForProduct(productId: String): Flow<List<ShopperReviewEntity>>

    @Query("SELECT * FROM shopper_reviews WHERE authorId = 'me'")
    fun observeMine(): Flow<List<ShopperReviewEntity>>

    @Query("SELECT * FROM shopper_reviews WHERE productId = :productId AND authorId = 'me'")
    suspend fun findMine(productId: String): ShopperReviewEntity?

    @Upsert
    suspend fun upsert(reviews: List<ShopperReviewEntity>)

    @Query("UPDATE shopper_reviews SET pending = 0 WHERE productId = :productId AND authorId = 'me' AND createdAt = :createdAt")
    suspend fun markSynced(productId: String, createdAt: Long)

    @Query("DELETE FROM shopper_reviews WHERE productId = :productId AND authorId = 'me' AND createdAt = :createdAt")
    suspend fun deleteMine(productId: String, createdAt: Long)

    @Query("DELETE FROM shopper_reviews WHERE authorId = 'me'")
    suspend fun deleteAllMine()
}
