package com.cikup.amazgone.account.domain.usecase

import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.sync.domain.OutboxStore
import kotlinx.coroutines.flow.first

/**
 * Signs out and wipes per-user local data. Refuses while changes are unsynced unless [force]
 * (the UI asks the user first), because those changes would otherwise be lost or pushed to the next account.
 */
class LogoutUseCase(
    private val auth: AuthRepository,
    private val outbox: OutboxStore,
    private val stores: List<UserScopedStore>,
) {
    suspend operator fun invoke(force: Boolean = false): DomainResult<Unit> {
        val pending = outbox.observePendingCount().first()
        if (pending > 0 && !force) return DomainResult.Failure(DomainError.PendingChanges(pending))
        auth.logout()
        stores.forEach { it.clearUserData() }
        return DomainResult.Success(Unit)
    }
}
