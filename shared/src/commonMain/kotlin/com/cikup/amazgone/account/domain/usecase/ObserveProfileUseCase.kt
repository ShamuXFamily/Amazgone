package com.cikup.amazgone.account.domain.usecase

import com.cikup.amazgone.account.domain.model.UserProfile
import com.cikup.amazgone.account.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveProfileUseCase(private val profiles: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = profiles.observeProfile()
}
