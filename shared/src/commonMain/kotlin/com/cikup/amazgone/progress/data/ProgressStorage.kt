package com.cikup.amazgone.progress.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "achievements")
data class AchievementEntity(@PrimaryKey val id: String, val unlockedAt: Long)

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity): Long

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}

@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val xp: Long,
    val level: Int,
    val rank: Int,
)

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY rank ASC")
    fun observeAll(): Flow<List<LeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LeaderboardEntity>)

    @Query("DELETE FROM leaderboard")
    suspend fun deleteAll()
}
