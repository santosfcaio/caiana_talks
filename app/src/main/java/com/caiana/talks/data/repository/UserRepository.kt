package com.caiana.talks.data.repository

import com.caiana.talks.data.local.db.UserProfileDao
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

interface UserRepository {
    fun getAllProfiles(): Flow<List<UserProfileEntity>>
    fun getActiveUserProfile(): Flow<UserProfileEntity?>
    suspend fun selectUser(id: Int)
    suspend fun clearActiveUser()
}

class UserRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val dao: UserProfileDao
) : UserRepository {

    override fun getAllProfiles(): Flow<List<UserProfileEntity>> = dao.getAll()

    override fun getActiveUserProfile(): Flow<UserProfileEntity?> =
        dataStore.activeUserId.flatMapLatest { id ->
            if (id == null) flowOf(null) else dao.getById(id)
        }

    override suspend fun selectUser(id: Int) {
        dataStore.setActiveUserId(id)
    }

    override suspend fun clearActiveUser() {
        dataStore.clearActiveUserId()
    }
}
