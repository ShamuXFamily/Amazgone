package com.cikup.amazgone.core.presentation.error

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.checkout_insufficient
import amazgone.shared.generated.resources.error_field_chars
import amazgone.shared.generated.resources.error_field_empty
import amazgone.shared.generated.resources.error_field_long
import amazgone.shared.generated.resources.error_field_short
import amazgone.shared.generated.resources.error_generic
import amazgone.shared.generated.resources.error_invalid_credentials
import amazgone.shared.generated.resources.error_network
import amazgone.shared.generated.resources.error_offline
import amazgone.shared.generated.resources.error_passwords_mismatch
import amazgone.shared.generated.resources.error_remote_unavailable
import amazgone.shared.generated.resources.error_too_many_attempts
import amazgone.shared.generated.resources.error_username_taken
import androidx.compose.runtime.Composable
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

/** The single place where domain errors become user-facing text. */
@Composable
fun DomainError.message(): String = when (this) {
    is DomainError.Validation -> reason.message()
    is DomainError.InsufficientCoins -> stringResource(Res.string.checkout_insufficient, Formatters.coins(required - available))
    DomainError.Network -> stringResource(Res.string.error_network)
    DomainError.Offline -> stringResource(Res.string.error_offline)
    DomainError.RemoteUnavailable -> stringResource(Res.string.error_remote_unavailable)
    DomainError.TooManyAttempts -> stringResource(Res.string.error_too_many_attempts)
    else -> stringResource(Res.string.error_generic)
}

@Composable
fun ValidationReason.message(): String = stringResource(
    when (this) {
        ValidationReason.EMPTY -> Res.string.error_field_empty
        ValidationReason.TOO_SHORT -> Res.string.error_field_short
        ValidationReason.TOO_LONG -> Res.string.error_field_long
        ValidationReason.INVALID_CHARACTERS -> Res.string.error_field_chars
        ValidationReason.TAKEN -> Res.string.error_username_taken
        ValidationReason.MISMATCH -> Res.string.error_passwords_mismatch
        ValidationReason.INVALID_CREDENTIALS -> Res.string.error_invalid_credentials
    },
)
