package com.cikup.amazgone.account.domain.usecase

import com.cikup.amazgone.account.domain.model.CredentialsValidator
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.domain.ValidationReason

/**
 * Signs in. If the previous login had expired and a *different* account signs in now, the old
 * account's local data (including its unsynced outbox) is wiped so it can never leak across accounts.
 */
class LoginUseCase(
    private val auth: AuthRepository,
    private val stores: List<UserScopedStore>,
) {
    suspend operator fun invoke(username: String, password: String): DomainResult<UserSession> {
        if (CredentialsValidator.normalizeUsername(username).isEmpty()) {
            return DomainResult.Failure(DomainError.Validation(CredentialsValidator.FIELD_USERNAME, ValidationReason.EMPTY))
        }
        if (password.isEmpty()) {
            return DomainResult.Failure(DomainError.Validation(CredentialsValidator.FIELD_PASSWORD, ValidationReason.EMPTY))
        }
        val expired = auth.expiredSession.value
        val result = auth.login(CredentialsValidator.normalizeUsername(username), password)
        if (result is DomainResult.Success && expired != null && expired.uid != result.value.uid) {
            stores.forEach { it.clearUserData() }
        }
        return result
    }
}
