package com.cikup.amazgone.account.domain.usecase

import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.account.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveSessionUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<UserSession?> = auth.session
}
