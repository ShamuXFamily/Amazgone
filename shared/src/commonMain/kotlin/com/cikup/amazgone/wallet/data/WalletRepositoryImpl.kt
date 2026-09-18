package com.cikup.amazgone.wallet.data

import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerEntry
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import com.cikup.amazgone.wallet.domain.model.LedgerStatus
import com.cikup.amazgone.wallet.domain.model.WalletRules
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Append-only ledger. Balances are always SUM(amount) over non-rejected rows, never a stored number.
 * Server state arrives as a BASELINE row that replaces all CONFIRMED rows; PENDING rows stay on top.
 */
class WalletRepositoryImpl(
    private val dao: LedgerDao,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : WalletRepository, UserScopedStore, StartupTask {
    override val name = "guest-wallet"

    override fun observeBalance(currency: Currency): Flow<Long> = dao.observeBalance(currency.name)
    override fun observePending(currency: Currency): Flow<Long> = dao.observePending(currency.name)
    override fun observeRecent(currency: Currency, limit: Int): Flow<List<LedgerEntry>> =
        dao.observeRecent(currency.name, limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun balance(currency: Currency): Long = dao.balance(currency.name)

    /** Records an optimistic local change tied to [refId] (order id, reward id…). Call inside a transaction. */
    suspend fun recordPending(currency: Currency, amount: Long, reason: LedgerReason, refId: String) {
        dao.insert(listOf(entity(currency, amount, reason, refId, LedgerStatus.PENDING)))
    }

    suspend fun lastEntryAt(reason: LedgerReason): Long? = dao.lastCreatedAt(reason.name)

    suspend fun confirm(refId: String) = dao.resolve(refId, LedgerStatus.CONFIRMED.name)

    /** Rejected rows stop counting: this is the refund / reward reversal. */
    suspend fun reject(refId: String) = dao.resolve(refId, LedgerStatus.REJECTED.name)

    suspend fun applyServerBaseline(coins: Long, xp: Long) = transactions.inTransaction {
        listOf(Currency.COINS to coins, Currency.XP to xp).forEach { (currency, amount) ->
            dao.deleteConfirmed(currency.name)
            dao.insert(listOf(entity(currency, amount, LedgerReason.BASELINE, null, LedgerStatus.CONFIRMED)))
        }
    }

    /** First launch (and after sign-out): guests get the starter coins locally. */
    override suspend fun run() {
        if (dao.count() == 0) grantGuestStarter()
    }

    override suspend fun clearUserData() = transactions.inTransaction {
        dao.deleteAll()
        grantGuestStarter()
    }

    private suspend fun grantGuestStarter() {
        dao.insert(listOf(entity(Currency.COINS, WalletRules.STARTER_COINS, LedgerReason.STARTER, null, LedgerStatus.CONFIRMED)))
    }

    private fun entity(currency: Currency, amount: Long, reason: LedgerReason, refId: String?, status: LedgerStatus) =
        LedgerEntity(ids.newId(), currency.name, amount, reason.name, refId, status.name, time.nowMillis())
}

private fun LedgerEntity.toDomain() = LedgerEntry(
    id = id,
    currency = Currency.valueOf(currency),
    amount = amount,
    reason = runCatching { LedgerReason.valueOf(reason) }.getOrDefault(LedgerReason.BASELINE),
    refId = refId,
    status = LedgerStatus.valueOf(status),
    createdAt = createdAt,
)
