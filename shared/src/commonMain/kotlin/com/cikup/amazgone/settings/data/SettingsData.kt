package com.cikup.amazgone.settings.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.cikup.amazgone.settings.domain.model.ThemeMode
import com.cikup.amazgone.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One row per preference. Device-level, so it is NOT cleared on sign-out and never synced. */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun upsert(setting: SettingEntity)
}

class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {
    override fun observeThemeMode(): Flow<ThemeMode> = dao.observe(KEY_THEME_MODE).map(ThemeMode::parse)

    override suspend fun setThemeMode(mode: ThemeMode) = dao.upsert(SettingEntity(KEY_THEME_MODE, mode.name))

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
