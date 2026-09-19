package com.cikup.amazgone.delivery.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A courier unlocked in the Garage (starters are implicit and never stored). */
@Entity(tableName = "owned_couriers")
data class OwnedCourierEntity(
    @PrimaryKey val courier: String,
    val purchaseId: String,
    val purchasedAt: Long,
    val pending: Boolean,
)

@Dao
interface OwnedCourierDao {
    @Query("SELECT * FROM owned_couriers")
    fun observeAll(): Flow<List<OwnedCourierEntity>>

    @Query("SELECT * FROM owned_couriers WHERE courier = :courier")
    suspend fun find(courier: String): OwnedCourierEntity?

    @Upsert
    suspend fun upsert(rows: List<OwnedCourierEntity>)

    @Query("UPDATE owned_couriers SET pending = 0 WHERE purchaseId = :purchaseId")
    suspend fun markSynced(purchaseId: String)

    @Query("DELETE FROM owned_couriers WHERE purchaseId = :purchaseId")
    suspend fun deletePurchase(purchaseId: String)

    @Query("DELETE FROM owned_couriers")
    suspend fun deleteAll()
}
