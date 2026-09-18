package com.cikup.amazgone.progress.domain.usecase

import com.cikup.amazgone.progress.domain.model.LevelCurve
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class Progress(val xp: Long, val level: Int, val levelProgress: Float, val nextLevelXp: Long)

/** XP (confirmed + pending) turned into level progress. */
class ObserveProgressUseCase(private val wallet: WalletRepository) {
    operator fun invoke(): Flow<Progress> = wallet.observeBalance(Currency.XP)
        .map { xp ->
            val level = LevelCurve.levelFor(xp)
            Progress(xp, level, LevelCurve.progress(xp), LevelCurve.xpForLevel(level + 1))
        }
        .distinctUntilChanged()
}
