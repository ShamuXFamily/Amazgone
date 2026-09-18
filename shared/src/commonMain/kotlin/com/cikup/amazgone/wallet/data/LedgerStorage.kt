package com.cikup.amazgone.wallet.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ledger", indices = [Index("refId"), Index("currency")])
data class LedgerEntity(
    @PrimaryKey val id: String,
    val currency: String,
    val amount: Long,
    val reason: String,
    val refId: String?,
    val status: String,
    val createdAt: Long,
)

@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(entries: List<LedgerEntity>)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE currency = :currency AND status != 'REJECTED'")
    fun observeBalance(currency: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE currency = :currency AND status != 'REJECTED'")
    suspend fun balance(currency: String): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE currency = :currency AND status = 'PENDING'")
    fun observePending(currency: String): Flow<Long>

    @Query("SELECT * FROM ledger WHERE currency = :currency AND reason != 'BASELINE' ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(currency: String, limit: Int): Flow<List<LedgerEntity>>

    @Query("UPDATE ledger SET status = :status WHERE refId = :refId AND status = 'PENDING'")
    suspend fun resolve(refId: String, status: String)

    @Query("DELETE FROM ledger WHERE currency = :currency AND status = 'CONFIRMED'")
    suspend fun deleteConfirmed(currency: String)

    @Query("SELECT MAX(createdAt) FROM ledger WHERE reason = :reason AND status != 'REJECTED'")
    suspend fun lastCreatedAt(reason: String): Long?

    @Query("SELECT COUNT(*) FROM ledger")
    suspend fun count(): Int

    @Query("DELETE FROM ledger")
    suspend fun deleteAll()
}
