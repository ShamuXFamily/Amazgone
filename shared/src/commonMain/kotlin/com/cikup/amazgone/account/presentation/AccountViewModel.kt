package com.cikup.amazgone.account.presentation

import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.account.domain.usecase.LoginUseCase
import com.cikup.amazgone.account.domain.usecase.LogoutUseCase
import com.cikup.amazgone.account.domain.usecase.ObserveProfileUseCase
import com.cikup.amazgone.account.domain.usecase.ObserveSessionUseCase
import com.cikup.amazgone.account.domain.usecase.RegisterUseCase
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.core.sync.domain.usecase.ObserveSyncStatusUseCase
import com.cikup.amazgone.core.sync.domain.usecase.RequestSyncUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase

class AccountViewModel(
    observeSession: ObserveSessionUseCase,
    observeProfile: ObserveProfileUseCase,
    observeWallet: ObserveWalletUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    observeProgress: ObserveProgressUseCase,
    auth: AuthRepository,
    private val login: LoginUseCase,
    private val register: RegisterUseCase,
    private val logout: LogoutUseCase,
    private val requestSync: RequestSyncUseCase,
) : MviViewModel<AccountState, AccountIntent, AccountEffect>(AccountState(isRemoteAvailable = auth.isRemoteAvailable)) {

    init {
        observeSession().observe { setState { copy(session = it) } }
        auth.expiredSession.observe { expired ->
            setState {
                copy(
                    expiredUsername = expired?.username,
                    form = if (expired != null && form.username.isEmpty()) form.copy(username = expired.username) else form,
                )
            }
        }
        observeProfile().observe { setState { copy(profile = it) } }
        observeWallet().observe { setState { copy(coins = it.coins, pendingCoins = it.pendingCoins) } }
        observeProgress().observe { setState { copy(xp = it.xp) } }
        observeSyncStatus().observe { setState { copy(syncStatus = it) } }
    }

    override fun handleIntent(intent: AccountIntent) {
        when (intent) {
            is AccountIntent.SwitchMode -> updateForm { AuthForm(mode = intent.mode, username = username) }
            is AccountIntent.UsernameChanged -> updateForm { copy(username = intent.value, fieldErrors = fieldErrors - FIELD_USERNAME) }
            is AccountIntent.PasswordChanged -> updateForm { copy(password = intent.value, fieldErrors = fieldErrors - FIELD_PASSWORD) }
            is AccountIntent.ConfirmationChanged -> updateForm { copy(confirmation = intent.value, fieldErrors = fieldErrors - FIELD_CONFIRMATION) }
            AccountIntent.Submit -> submit()
            AccountIntent.SyncNow -> requestSync(force = true)
            AccountIntent.SignOut -> signOut(force = false)
            AccountIntent.ConfirmSignOut -> signOut(force = true)
            AccountIntent.DismissSignOut -> setState { copy(pendingSignOutCount = null) }
            is AccountIntent.Open -> sendEffect(AccountEffect.Navigate(intent.destination))
        }
    }

    private fun submit() {
        val form = currentState.form
        if (form.isSubmitting) return
        updateForm { copy(isSubmitting = true, formError = null) }
        launchSafely {
            val result = when (form.mode) {
                AuthMode.SIGN_IN -> login(form.username, form.password)
                AuthMode.REGISTER -> register(form.username, form.password, form.confirmation)
            }
            when (result) {
                is DomainResult.Success -> {
                    setState { copy(form = AuthForm()) }
                    requestSync(force = true)
                    sendEffect(AccountEffect.SignedIn)
                }
                is DomainResult.Failure -> updateForm { withError(result.error) }
            }
        }
    }

    private fun signOut(force: Boolean) = launchSafely {
        when (val result = logout(force)) {
            is DomainResult.Success -> setState { copy(pendingSignOutCount = null, profile = null) }
            is DomainResult.Failure -> (result.error as? DomainError.PendingChanges)?.let { pending ->
                setState { copy(pendingSignOutCount = pending.count) }
            }
        }
    }

    override fun onError(throwable: Throwable) = updateForm { withError(DomainError.Unknown(throwable.message)) }

    private fun updateForm(reduce: AuthForm.() -> AuthForm) = setState { copy(form = form.reduce()) }

    private fun AuthForm.withError(error: DomainError): AuthForm = copy(
        isSubmitting = false,
        errorPulse = errorPulse + 1,
        fieldErrors = if (error is DomainError.Validation) fieldErrors + (error.field to error) else fieldErrors,
        formError = error.takeUnless { it is DomainError.Validation },
    )

    private companion object {
        const val FIELD_USERNAME = "username"
        const val FIELD_PASSWORD = "password"
        const val FIELD_CONFIRMATION = "confirmation"
    }
}
