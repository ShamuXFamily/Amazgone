package com.cikup.amazgone.wallet.domain.repository

import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    /** Sum of all non-rejected entries (confirmed + pending). */
    fun observeBalance(currency: Currency): Flow<Long>
    fun observePending(currency: Currency): Flow<Long>
    fun observeRecent(currency: Currency, limit: Int): Flow<List<LedgerEntry>>
    suspend fun balance(currency: Currency): Long
}
