package com.cikup.amazgone.orders.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val status: String,
    val subtotalCoins: Long,
    val discountCoins: Long,
    val totalCoins: Long,
    val couponCode: String?,
    val deliveryOption: String,
    val deliveryFeeCoins: Long,
    val addressJson: String,
    val itemsJson: String,
    val xpEarned: Long,
    val createdAt: Long,
    val rejectionReason: String?,
    val deliveredAt: Long? = null,
    val courier: String? = null,
)

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun observe(id: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun find(id: String): OrderEntity?

    @Query("UPDATE orders SET status = :status, rejectionReason = :reason WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, reason: String?)

    @Query("UPDATE orders SET deliveredAt = :at WHERE id = :id AND deliveredAt IS NULL")
    suspend fun markDelivered(id: String, at: Long): Int

    @Query("SELECT COUNT(*) FROM orders WHERE status != 'REJECTED'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalCoins), 0) FROM orders WHERE status != 'REJECTED'")
    fun observeSpent(): Flow<Long>

    @Query("DELETE FROM orders")
    suspend fun deleteAll()
}
