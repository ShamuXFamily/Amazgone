package com.cikup.amazgone.account.domain.model

import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.ValidationReason

/**
 * Username/password rules, applied at the UI boundary before any network call.
 * Usernames are case-insensitive: they are normalised to lowercase.
 */
object CredentialsValidator {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 128
    private val USERNAME_PATTERN = Regex("^[a-z0-9_]+$")

    fun normalizeUsername(raw: String): String = raw.trim().lowercase()

    fun validateUsername(raw: String): DomainError.Validation? {
        val username = normalizeUsername(raw)
        val reason = when {
            username.isEmpty() -> ValidationReason.EMPTY
            username.length < USERNAME_MIN -> ValidationReason.TOO_SHORT
            username.length > USERNAME_MAX -> ValidationReason.TOO_LONG
            !USERNAME_PATTERN.matches(username) -> ValidationReason.INVALID_CHARACTERS
            else -> return null
        }
        return DomainError.Validation(FIELD_USERNAME, reason)
    }

    fun validatePassword(password: String, confirmation: String? = null): DomainError.Validation? {
        val reason = when {
            password.isEmpty() -> ValidationReason.EMPTY
            password.length < PASSWORD_MIN -> ValidationReason.TOO_SHORT
            password.length > PASSWORD_MAX -> ValidationReason.TOO_LONG
            confirmation != null && confirmation != password -> ValidationReason.MISMATCH
            else -> return null
        }
        return DomainError.Validation(if (reason == ValidationReason.MISMATCH) FIELD_CONFIRMATION else FIELD_PASSWORD, reason)
    }

    /** Firebase Auth is email-based; each username maps to a private synthetic address. */
    fun syntheticEmail(username: String): String = "${normalizeUsername(username)}@$EMAIL_DOMAIN"

    const val FIELD_USERNAME = "username"
    const val FIELD_PASSWORD = "password"
    const val FIELD_CONFIRMATION = "confirmation"
    private const val EMAIL_DOMAIN = "amazgone.local"
}
