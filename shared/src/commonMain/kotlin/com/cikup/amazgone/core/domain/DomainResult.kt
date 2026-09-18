package com.cikup.amazgone.core.domain

/** Result of a domain operation. Presentation maps [DomainError] to user-facing strings. */
sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

sealed interface DomainError {
    data object Network : DomainError
    data object NotFound : DomainError
    data object NotSignedIn : DomainError
    data object Offline : DomainError
    data class InsufficientCoins(val required: Long, val available: Long) : DomainError
    data class Validation(val field: String, val reason: ValidationReason) : DomainError
    data class Cooldown(val remainingMillis: Long) : DomainError
    /** Firebase is not configured for this build/project, so online features are unavailable. */
    data object RemoteUnavailable : DomainError
    data object TooManyAttempts : DomainError
    /** Local changes that would be lost (e.g. signing out before they synced). */
    data class PendingChanges(val count: Int) : DomainError
    data class Unknown(val message: String?) : DomainError
}

enum class ValidationReason { EMPTY, TOO_SHORT, TOO_LONG, INVALID_CHARACTERS, TAKEN, MISMATCH, INVALID_CREDENTIALS }

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}
