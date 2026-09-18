package com.cikup.amazgone.wallet.domain.usecase

import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.WalletSummary
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveWalletUseCase(private val wallet: WalletRepository) {
    operator fun invoke(recentLimit: Int = RECENT_LIMIT): Flow<WalletSummary> = combine(
        wallet.observeBalance(Currency.COINS),
        wallet.observePending(Currency.COINS),
        wallet.observeRecent(Currency.COINS, recentLimit),
    ) { balance, pending, recent -> WalletSummary(balance, pending, recent) }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
