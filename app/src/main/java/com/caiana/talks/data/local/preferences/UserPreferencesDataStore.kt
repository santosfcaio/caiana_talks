package com.caiana.talks.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

interface UserPreferencesDataStore {
    val activeUserId: Flow<Int?>
    suspend fun setActiveUserId(id: Int)
    suspend fun clearActiveUserId()
}

class UserPreferencesDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesDataStore {

    private val activeUserIdKey = intPreferencesKey("active_user_id")

    override val activeUserId: Flow<Int?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { preferences -> preferences[activeUserIdKey] }

    override suspend fun setActiveUserId(id: Int) {
        dataStore.edit { preferences -> preferences[activeUserIdKey] = id }
    }

    override suspend fun clearActiveUserId() {
        dataStore.edit { preferences -> preferences.remove(activeUserIdKey) }
    }
}
