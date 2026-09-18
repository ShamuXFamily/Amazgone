package com.cikup.amazgone.core.sync.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    /** IGNORE keeps enqueue idempotent if the same mutation id is written twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: OutboxEntity)

    @Query("SELECT * FROM outbox WHERE parked = 0 ORDER BY seq ASC LIMIT :limit")
    suspend fun head(limit: Int): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE seq = :seq")
    suspend fun delete(seq: Long)

    @Query("UPDATE outbox SET attempts = :attempts, nextAttemptAt = :nextAttemptAt, lastError = :error WHERE seq = :seq")
    suspend fun scheduleRetry(seq: Long, attempts: Int, nextAttemptAt: Long, error: String)

    @Query("UPDATE outbox SET parked = 1, lastError = :error WHERE seq = :seq")
    suspend fun park(seq: Long, error: String)

    @Query("UPDATE outbox SET parked = 0, attempts = 0, nextAttemptAt = 0 WHERE parked = 1")
    suspend fun unparkAll()

    @Query("DELETE FROM outbox")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeCount(): Flow<Int>
}

@Dao
interface SyncMetaDao {
    @Query("SELECT lastSyncedAt FROM sync_meta WHERE `key` = :key")
    suspend fun lastSyncedAt(key: String): Long?

    @Query("SELECT lastSyncedAt FROM sync_meta WHERE `key` = :key")
    fun observeLastSyncedAt(key: String): Flow<Long?>

    @Upsert
    suspend fun upsert(entity: SyncMetaEntity)
}
