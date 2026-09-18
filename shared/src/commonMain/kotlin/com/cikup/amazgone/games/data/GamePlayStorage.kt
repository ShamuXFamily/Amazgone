package com.cikup.amazgone.games.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_plays")
data class GamePlayEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val rewardIndex: Int,
    val coins: Long,
    val xp: Long,
    val playedAt: Long,
)

@Dao
interface GamePlayDao {
    @Insert
    suspend fun insert(play: GamePlayEntity)

    @Query("SELECT MAX(playedAt) FROM game_plays WHERE kind = :kind")
    fun observeLastPlayedAt(kind: String): Flow<Long?>

    @Query("SELECT MAX(playedAt) FROM game_plays WHERE kind = :kind")
    suspend fun lastPlayedAt(kind: String): Long?

    @Query("SELECT * FROM game_plays WHERE kind = :kind ORDER BY playedAt DESC LIMIT 1")
    fun observeLatest(kind: String): Flow<GamePlayEntity?>

    @Query("SELECT COUNT(*) FROM game_plays WHERE kind = :kind")
    fun observeCount(kind: String): Flow<Int>

    @Query("DELETE FROM game_plays")
    suspend fun deleteAll()
}
