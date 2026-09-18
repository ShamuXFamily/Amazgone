package com.cikup.amazgone.cart.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** quantity == 0 is a tombstone kept so a removal can win last-write-wins sync. */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val quantity: Int,
    val addedAt: Long,
    val updatedAt: Long,
)

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE quantity > 0 ORDER BY addedAt ASC")
    fun observeActive(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun find(productId: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE quantity > 0")
    suspend fun active(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items")
    suspend fun all(): List<CartItemEntity>

    @Upsert
    suspend fun upsert(item: CartItemEntity)
}
