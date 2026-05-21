package com.caiana.talks.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.caiana.talks.data.local.preferences.UserPreferencesDataStoreImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserPreferencesDataStoreTest {

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var impl: UserPreferencesDataStoreImpl

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        impl = UserPreferencesDataStoreImpl(fakeDataStore)
    }

    @Test
    fun `openrouterApiKey emits empty string by default`() = runTest {
        assertEquals("", impl.openrouterApiKey.first())
    }

    @Test
    fun `setOpenrouterApiKey stores value and openrouterApiKey emits it`() = runTest {
        impl.setOpenrouterApiKey("my-key")
        assertEquals("my-key", impl.openrouterApiKey.first())
    }

    @Test
    fun `aiModel emits empty string by default`() = runTest {
        assertEquals("", impl.aiModel.first())
    }

    @Test
    fun `setAiModel stores value and aiModel emits it`() = runTest {
        impl.setAiModel("qwen/qwen3.5-9b")
        assertEquals("qwen/qwen3.5-9b", impl.aiModel.first())
    }

    private class FakeDataStore : DataStore<Preferences> {
        private val prefsFlow = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = prefsFlow
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(prefsFlow.value)
            prefsFlow.value = updated
            return updated
        }
    }
}
