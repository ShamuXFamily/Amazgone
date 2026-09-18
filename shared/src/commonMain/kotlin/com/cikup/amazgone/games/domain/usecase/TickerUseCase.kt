package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.core.common.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits the current time every [periodMillis] for countdowns. */
class TickerUseCase(private val time: TimeProvider) {
    operator fun invoke(periodMillis: Long = DEFAULT_PERIOD_MS): Flow<Long> = flow {
        while (true) {
            emit(time.nowMillis())
            delay(periodMillis)
        }
    }

    private companion object {
        const val DEFAULT_PERIOD_MS = 1_000L
    }
}
