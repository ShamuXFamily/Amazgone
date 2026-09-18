package com.cikup.amazgone.account.domain.usecase

import com.cikup.amazgone.account.domain.model.CredentialsValidator
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.account.domain.repository.ProfileRepository
import com.cikup.amazgone.core.domain.DomainResult

/** Online-only: validates, creates the Firebase account, then the server profile with starter coins. */
class RegisterUseCase(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) {
    suspend operator fun invoke(username: String, password: String, confirmation: String): DomainResult<UserSession> {
        CredentialsValidator.validateUsername(username)?.let { return DomainResult.Failure(it) }
        CredentialsValidator.validatePassword(password, confirmation)?.let { return DomainResult.Failure(it) }
        val registered = auth.register(CredentialsValidator.normalizeUsername(username), password)
        if (registered !is DomainResult.Success) return registered
        return when (val created = profiles.createRemoteProfile(registered.value)) {
            is DomainResult.Success -> registered
            is DomainResult.Failure -> created
        }
    }
}
