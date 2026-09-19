package com.cikup.amazgone.delivery.data

import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.repository.CourierRepository
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

const val OUTBOX_COURIER_BUY = "courier.buy"

@Serializable
data class CourierPurchasePayload(val purchaseId: String, val courier: String, val priceCoins: Long, val purchasedAt: Long)

class CourierRepositoryImpl(
    private val dao: OwnedCourierDao,
    private val wallet: WalletRepositoryImpl,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : CourierRepository, UserScopedStore {

    override fun observeOwned(): Flow<Set<Courier>> = dao.observeAll().map { rows ->
        Courier.STARTERS + rows.mapNotNull { Courier.parse(it.courier) }
    }

    override fun observePurchases(): Flow<Map<Courier, Long>> = dao.observeAll().map { rows ->
        rows.mapNotNull { row -> Courier.parse(row.courier)?.let { it to row.purchasedAt } }.toMap()
    }

    override suspend fun buy(courier: Courier): DomainResult<Unit> = transactions.inTransaction {
        if (courier.isStarter || dao.find(courier.name) != null) return@inTransaction DomainResult.Success(Unit)
        val balance = wallet.balance(Currency.COINS)
        if (balance < courier.priceCoins) {
            return@inTransaction DomainResult.Failure(DomainError.InsufficientCoins(courier.priceCoins, balance))
        }
        val payload = CourierPurchasePayload(ids.newId(), courier.name, courier.priceCoins, time.nowMillis())
        dao.upsert(listOf(OwnedCourierEntity(courier.name, payload.purchaseId, payload.purchasedAt, pending = true)))
        wallet.recordPending(Currency.COINS, -courier.priceCoins, LedgerReason.COURIER, payload.purchaseId)
        outbox.enqueue(payload.purchaseId, OUTBOX_COURIER_BUY, AppJson.encodeToString(CourierPurchasePayload.serializer(), payload))
        DomainResult.Success(Unit)
    }

    suspend fun markConfirmed(purchaseId: String) = transactions.inTransaction {
        dao.markSynced(purchaseId)
        wallet.confirm(purchaseId)
    }

    /** Server refused (not enough coins there): refund and lock the courier again. */
    suspend fun markRejected(purchaseId: String) = transactions.inTransaction {
        dao.deletePurchase(purchaseId)
        wallet.reject(purchaseId)
    }

    /** Couriers bought on another device. */
    suspend fun upsertRemote(courier: Courier, purchaseId: String, purchasedAt: Long) {
        if (dao.find(courier.name) == null) dao.upsert(listOf(OwnedCourierEntity(courier.name, purchaseId, purchasedAt, pending = false)))
    }

    override suspend fun clearUserData() = dao.deleteAll()
}
