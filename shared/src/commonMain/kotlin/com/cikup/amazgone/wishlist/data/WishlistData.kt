package com.cikup.amazgone.wishlist.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.cikup.amazgone.account.data.remote.UserDocuments
import com.cikup.amazgone.account.data.sync.pushResultOf
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.bool
import com.cikup.amazgone.core.remote.long
import com.cikup.amazgone.core.remote.string
import com.cikup.amazgone.core.sync.domain.OutboxEntry
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val OUTBOX_WISHLIST_SET = "wishlist.set"

/** saved=false rows are tombstones so removals win last-write-wins sync. */
@Entity(tableName = "wishlist")
data class WishlistEntity(@PrimaryKey val productId: String, val saved: Boolean, val updatedAt: Long)

@Dao
interface WishlistDao {
    @Query("SELECT productId FROM wishlist WHERE saved = 1 ORDER BY updatedAt DESC")
    fun observeSavedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM wishlist WHERE saved = 1")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM wishlist WHERE productId = :productId")
    suspend fun find(productId: String): WishlistEntity?

    @Upsert
    suspend fun upsert(entity: WishlistEntity)

    @Query("DELETE FROM wishlist")
    suspend fun deleteAll()
}

@Serializable
data class WishlistSetPayload(val productId: String, val saved: Boolean, val updatedAt: Long)

class WishlistRepositoryImpl(
    private val dao: WishlistDao,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : WishlistRepository, UserScopedStore {
    override fun observeIds(): Flow<List<String>> = dao.observeSavedIds()

    override suspend fun isSaved(productId: String): Boolean = dao.find(productId)?.saved == true

    override suspend fun setSaved(productId: String, saved: Boolean) = transactions.inTransaction {
        val now = time.nowMillis()
        dao.upsert(WishlistEntity(productId, saved, now))
        outbox.enqueue(ids.newId(), OUTBOX_WISHLIST_SET, AppJson.encodeToString(WishlistSetPayload.serializer(), WishlistSetPayload(productId, saved, now)))
    }

    suspend fun mergeRemote(productId: String, saved: Boolean, updatedAt: Long) {
        val local = dao.find(productId)
        if (local == null || local.updatedAt < updatedAt) dao.upsert(WishlistEntity(productId, saved, updatedAt))
    }

    override suspend fun clearUserData() = dao.deleteAll()
}

class WishlistSetHandler(private val auth: AuthRepository, private val firebase: FirebaseServices) : OutboxHandler {
    override val type = OUTBOX_WISHLIST_SET

    override suspend fun push(entry: OutboxEntry): PushResult {
        val uid = auth.session.value?.uid ?: return PushResult.Retry("signed out")
        val payload = AppJson.decodeFromString(WishlistSetPayload.serializer(), entry.payload)
        return pushResultOf {
            firebase.requireFirestore().commit(
                listOf(
                    FirestoreWrite.Set(
                        UserDocuments.wishlistItem(uid, payload.productId),
                        mapOf(FIELD_PRODUCT to payload.productId, FIELD_SAVED to payload.saved, FIELD_UPDATED to payload.updatedAt),
                    ),
                ),
            )
        }
    }

    override suspend fun onRejected(entry: OutboxEntry, reason: String) = Unit
}

class WishlistPuller(
    private val auth: AuthRepository,
    private val firebase: FirebaseServices,
    private val repository: WishlistRepositoryImpl,
) : RemotePuller {
    override val name = "wishlist"
    override val requiresAuth = true

    override suspend fun pull(force: Boolean) {
        val uid = auth.session.value?.uid ?: return
        firebase.requireFirestore().list(UserDocuments.wishlist(uid)).forEach { doc ->
            val productId = doc.fields.string(FIELD_PRODUCT) ?: return@forEach
            repository.mergeRemote(productId, doc.fields.bool(FIELD_SAVED), doc.fields.long(FIELD_UPDATED))
        }
    }
}

private const val FIELD_PRODUCT = "productId"
private const val FIELD_SAVED = "saved"
private const val FIELD_UPDATED = "updatedAt"
