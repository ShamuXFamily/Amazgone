package com.cikup.amazgone.account.data.repository

import com.cikup.amazgone.account.data.local.UserProfileDao
import com.cikup.amazgone.account.data.local.UserProfileEntity
import com.cikup.amazgone.account.domain.model.UserProfile
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.account.domain.repository.ProfileRepository
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.sync.domain.OutboxStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

const val OUTBOX_PROFILE_CREATE = "profile.create"

@Serializable
data class ProfileCreatePayload(val uid: String, val username: String)

class ProfileRepositoryImpl(
    private val dao: UserProfileDao,
    private val outbox: OutboxStore,
    private val ids: IdGenerator,
) : ProfileRepository, UserScopedStore {

    override fun observeProfile(): Flow<UserProfile?> = dao.observe().map { it?.toDomain() }

    /** Queued (not called inline) so registration succeeds even if Firestore is briefly unreachable. */
    override suspend fun createRemoteProfile(session: UserSession): DomainResult<Unit> {
        outbox.enqueue(
            ids.newId(),
            OUTBOX_PROFILE_CREATE,
            AppJson.encodeToString(ProfileCreatePayload.serializer(), ProfileCreatePayload(session.uid, session.username)),
        )
        return DomainResult.Success(Unit)
    }

    suspend fun save(profile: UserProfileEntity) = dao.upsert(profile)

    suspend fun current(): UserProfileEntity? = dao.current()

    override suspend fun clearUserData() = dao.deleteAll()
}

private fun UserProfileEntity.toDomain() =
    UserProfile(uid, username, orderCount, lastSpinAt, lastScratchAt, createdAt, syncedAt)
