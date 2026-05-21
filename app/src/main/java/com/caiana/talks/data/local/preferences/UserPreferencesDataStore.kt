package com.caiana.talks.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

interface UserPreferencesDataStore {
    val activeUserId: Flow<Int?>
    val openrouterApiKey: Flow<String>
    val aiModel: Flow<String>

    suspend fun setActiveUserId(id: Int)
    suspend fun clearActiveUserId()
    suspend fun setOpenrouterApiKey(key: String)
    suspend fun setAiModel(model: String)
}

class UserPreferencesDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesDataStore {

    private val activeUserIdKey = intPreferencesKey("active_user_id")
    private val openrouterApiKeyKey = stringPreferencesKey("openrouter_api_key")
    private val aiModelKey = stringPreferencesKey("ai_model")

    override val activeUserId: Flow<Int?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { preferences -> preferences[activeUserIdKey] }

    override val openrouterApiKey: Flow<String> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { preferences -> preferences[openrouterApiKeyKey] ?: "" }

    override val aiModel: Flow<String> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { preferences -> preferences[aiModelKey] ?: "" }

    override suspend fun setActiveUserId(id: Int) {
        dataStore.edit { preferences -> preferences[activeUserIdKey] = id }
    }

    override suspend fun clearActiveUserId() {
        dataStore.edit { preferences -> preferences.remove(activeUserIdKey) }
    }

    override suspend fun setOpenrouterApiKey(key: String) {
        dataStore.edit { preferences -> preferences[openrouterApiKeyKey] = key }
    }

    override suspend fun setAiModel(model: String) {
        dataStore.edit { preferences -> preferences[aiModelKey] = model }
    }
}
