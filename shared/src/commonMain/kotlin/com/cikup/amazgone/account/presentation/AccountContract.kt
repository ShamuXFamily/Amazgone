package com.cikup.amazgone.account.presentation

import com.cikup.amazgone.account.domain.model.UserProfile
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.core.sync.domain.SyncStatus

enum class AuthMode { SIGN_IN, REGISTER }

data class AuthForm(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val username: String = "",
    val password: String = "",
    val confirmation: String = "",
    val fieldErrors: Map<String, DomainError.Validation> = emptyMap(),
    val formError: DomainError? = null,
    val isSubmitting: Boolean = false,
    /** Increments on each failed submit to trigger the shake animation. */
    val errorPulse: Int = 0,
)

data class AccountState(
    val session: UserSession? = null,
    val profile: UserProfile? = null,
    val coins: Long = 0,
    val pendingCoins: Long = 0,
    val xp: Long = 0,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val isRemoteAvailable: Boolean = true,
    val form: AuthForm = AuthForm(),
    val pendingSignOutCount: Int? = null,
    val expiredUsername: String? = null,
) : UiState

sealed interface AccountIntent : UiIntent {
    data class SwitchMode(val mode: AuthMode) : AccountIntent
    data class UsernameChanged(val value: String) : AccountIntent
    data class PasswordChanged(val value: String) : AccountIntent
    data class ConfirmationChanged(val value: String) : AccountIntent
    data object Submit : AccountIntent
    data object SyncNow : AccountIntent
    data object SignOut : AccountIntent
    data object ConfirmSignOut : AccountIntent
    data object DismissSignOut : AccountIntent
    data class Open(val destination: AccountDestination) : AccountIntent
}

enum class AccountDestination { ORDERS, WISHLIST, WALLET, LEADERBOARD, ACHIEVEMENTS, SEARCH, GAMES, SPIN, CATEGORIES }

sealed interface AccountEffect : UiEffect {
    data class Navigate(val destination: AccountDestination) : AccountEffect
    data object SignedIn : AccountEffect
}
