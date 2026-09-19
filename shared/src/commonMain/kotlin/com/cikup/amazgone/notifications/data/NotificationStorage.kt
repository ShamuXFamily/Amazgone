package com.cikup.amazgone.notifications.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Inbox row. Device-level: kept across sign-out like settings (it describes this device's history). */
@Entity(tableName = "notifications", indices = [Index("deliverAt")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val link: String,
    val deliverAt: Long,
    val read: Boolean,
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY deliverAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT id FROM notifications")
    suspend fun ids(): List<String>

    @Query("SELECT id FROM notifications WHERE deliverAt > :now")
    suspend fun futureIds(now: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rows: List<NotificationEntity>)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET read = 1 WHERE deliverAt <= :upTo")
    suspend fun markAllRead(upTo: Long)

    @Query("SELECT MAX(deliverAt) FROM notifications WHERE id LIKE :prefix || '%'")
    suspend fun lastDeliverAt(prefix: String): Long?
}
