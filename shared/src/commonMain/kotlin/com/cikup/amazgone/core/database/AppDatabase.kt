package com.cikup.amazgone.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cikup.amazgone.account.data.local.UserProfileDao
import com.cikup.amazgone.account.data.local.UserProfileEntity
import com.cikup.amazgone.cart.data.coupon.CouponDao
import com.cikup.amazgone.cart.data.coupon.CouponEntity
import com.cikup.amazgone.cart.data.local.CartDao
import com.cikup.amazgone.cart.data.local.CartItemEntity
import com.cikup.amazgone.catalog.data.local.CatalogConverters
import com.cikup.amazgone.catalog.data.local.CatalogDao
import com.cikup.amazgone.catalog.data.local.ProductEntity
import com.cikup.amazgone.catalog.data.local.ProductFtsEntity
import com.cikup.amazgone.catalog.data.local.ReviewEntity
import com.cikup.amazgone.core.sync.data.OutboxDao
import com.cikup.amazgone.core.sync.data.OutboxEntity
import com.cikup.amazgone.core.sync.data.SyncMetaDao
import com.cikup.amazgone.core.sync.data.SyncMetaEntity
import com.cikup.amazgone.games.data.GamePlayDao
import com.cikup.amazgone.games.data.GamePlayEntity
import com.cikup.amazgone.orders.data.OrderDao
import com.cikup.amazgone.progress.data.AchievementDao
import com.cikup.amazgone.progress.data.AchievementEntity
import com.cikup.amazgone.progress.data.LeaderboardDao
import com.cikup.amazgone.progress.data.LeaderboardEntity
import com.cikup.amazgone.orders.data.OrderEntity
import com.cikup.amazgone.settings.data.SettingEntity
import com.cikup.amazgone.settings.data.SettingsDao
import com.cikup.amazgone.wallet.data.LedgerDao
import com.cikup.amazgone.wallet.data.LedgerEntity
import com.cikup.amazgone.wishlist.data.WishlistDao
import com.cikup.amazgone.wishlist.data.WishlistEntity
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Single local source of truth.
 * Pre-release policy: bump [SCHEMA_VERSION] on EVERY schema change; the destructive fallback in
 * [build] then wipes and recreates local data (the seed + sync refill it). Room only falls back on a
 * version change, never on a same-version hash mismatch. Add real migrations before public release.
 */
@Database(
    entities = [
        OutboxEntity::class,
        SyncMetaEntity::class,
        ProductEntity::class,
        ProductFtsEntity::class,
        ReviewEntity::class,
        CartItemEntity::class,
        CouponEntity::class,
        LedgerEntity::class,
        UserProfileEntity::class,
        OrderEntity::class,
        WishlistEntity::class,
        GamePlayEntity::class,
        AchievementEntity::class,
        LeaderboardEntity::class,
        SettingEntity::class,
    ],
    version = AppDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(CatalogConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun catalogDao(): CatalogDao
    abstract fun cartDao(): CartDao
    abstract fun couponDao(): CouponDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun orderDao(): OrderDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun gamePlayDao(): GamePlayDao
    abstract fun achievementDao(): AchievementDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val FILE_NAME = "amazgone.db"
        const val SCHEMA_VERSION = 9

        fun build(builder: Builder<AppDatabase>, dispatcher: CoroutineDispatcher): AppDatabase = builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(dispatcher)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
