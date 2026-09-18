package com.cikup.amazgone.cart.data.coupon

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.cart.domain.repository.CouponRepository
import com.cikup.amazgone.core.common.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val code: String,
    val kind: String,
    val value: Long,
    val minSubtotalCoins: Long,
    val expiresAt: Long?,
    val grantedAt: Long,
    val usedAt: Long?,
)

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons WHERE usedAt IS NULL AND (expiresAt IS NULL OR expiresAt > :now) ORDER BY grantedAt DESC")
    fun observeAvailable(now: Long): Flow<List<CouponEntity>>

    @Upsert
    suspend fun upsert(coupon: CouponEntity)

    @Query("UPDATE coupons SET usedAt = :usedAt WHERE code = :code")
    suspend fun markUsed(code: String, usedAt: Long)

    @Query("UPDATE coupons SET usedAt = NULL WHERE code = :code")
    suspend fun markUnused(code: String)

    @Query("DELETE FROM coupons")
    suspend fun deleteAll()
}

class CouponRepositoryImpl(private val dao: CouponDao, private val time: TimeProvider) : CouponRepository, UserScopedStore {
    override fun observeAvailable(now: Long): Flow<List<Coupon>> =
        dao.observeAvailable(now).map { rows -> rows.map { it.toDomain() } }

    override suspend fun grant(coupon: Coupon) = dao.upsert(
        CouponEntity(coupon.code, coupon.kind.name, coupon.value, coupon.minSubtotalCoins, coupon.expiresAt, time.nowMillis(), null),
    )

    suspend fun markUsed(code: String) = dao.markUsed(code, time.nowMillis())

    /** Gives a coupon back when the order that used it is rejected. */
    suspend fun markUnused(code: String) = dao.markUnused(code)

    override suspend fun clearUserData() = dao.deleteAll()
}

private fun CouponEntity.toDomain() = Coupon(code, CouponKind.valueOf(kind), value, minSubtotalCoins, expiresAt)
