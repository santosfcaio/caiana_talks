package com.caiana.talks.data

import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.local.db.UserProfileDao
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var fakeDataStore: FakeUserPreferencesDataStore
    private lateinit var fakeDao: FakeUserProfileDao
    private lateinit var repository: UserRepositoryImpl

    private val caioProfile = UserProfileEntity(id = 1, name = "Caio")
    private val anaProfile = UserProfileEntity(id = 2, name = "Ana")

    @Before
    fun setUp() {
        fakeDataStore = FakeUserPreferencesDataStore()
        fakeDao = FakeUserProfileDao(listOf(caioProfile, anaProfile))
        repository = UserRepositoryImpl(fakeDataStore, fakeDao)
    }

    // --- getActiveUserProfile ---

    @Test
    fun `getActiveUserProfile emits null when no user is selected`() = runTest {
        // Arrange
        fakeDataStore.activeUserIdFlow.value = null

        // Act
        val result = repository.getActiveUserProfile().first()

        // Assert
        assertNull(result)
    }

    @Test
    fun `getActiveUserProfile emits Caio when id 1 is stored`() = runTest {
        // Arrange
        fakeDataStore.activeUserIdFlow.value = 1

        // Act
        val result = repository.getActiveUserProfile().first()

        // Assert
        assertEquals(caioProfile, result)
    }

    @Test
    fun `getActiveUserProfile emits Ana when id 2 is stored`() = runTest {
        // Arrange
        fakeDataStore.activeUserIdFlow.value = 2

        // Act
        val result = repository.getActiveUserProfile().first()

        // Assert
        assertEquals(anaProfile, result)
    }

    // --- selectUser ---

    @Test
    fun `selectUser saves given id to datastore`() = runTest {
        // Arrange
        val expectedId = 1

        // Act
        repository.selectUser(expectedId)

        // Assert
        assertEquals(expectedId, fakeDataStore.lastSavedId)
    }

    @Test
    fun `selectUser with id 2 saves 2 to datastore`() = runTest {
        // Arrange
        val expectedId = 2

        // Act
        repository.selectUser(expectedId)

        // Assert
        assertEquals(expectedId, fakeDataStore.lastSavedId)
    }

    // --- clearActiveUser ---

    @Test
    fun `clearActiveUser calls clear on datastore`() = runTest {
        // Arrange
        fakeDataStore.activeUserIdFlow.value = 1

        // Act
        repository.clearActiveUser()

        // Assert
        assertTrue(fakeDataStore.clearCalled)
    }

    @Test
    fun `getActiveUserProfile emits null after clearActiveUser`() = runTest {
        // Arrange
        fakeDataStore.activeUserIdFlow.value = 1
        repository.clearActiveUser()

        // Act
        val result = repository.getActiveUserProfile().first()

        // Assert
        assertNull(result)
    }

    // --- getAllProfiles ---

    @Test
    fun `getAllProfiles emits all seeded profiles`() = runTest {
        // Arrange — profiles set in setUp

        // Act
        val result = repository.getAllProfiles().first()

        // Assert
        assertEquals(listOf(caioProfile, anaProfile), result)
    }

    // --- Fakes ---

    private class FakeUserPreferencesDataStore : UserPreferencesDataStore {
        val activeUserIdFlow = MutableStateFlow<Int?>(null)
        var lastSavedId: Int? = null
        var clearCalled = false

        override val activeUserId: Flow<Int?> = activeUserIdFlow

        override suspend fun setActiveUserId(id: Int) {
            lastSavedId = id
            activeUserIdFlow.value = id
        }

        override suspend fun clearActiveUserId() {
            clearCalled = true
            activeUserIdFlow.value = null
        }
    }

    private class FakeUserProfileDao(
        private val profiles: List<UserProfileEntity>
    ) : UserProfileDao {
        var lastUpdated: UserProfileEntity? = null

        override fun getAll(): Flow<List<UserProfileEntity>> = flowOf(profiles)
        override fun getById(id: Int): Flow<UserProfileEntity?> =
            flowOf(profiles.firstOrNull { it.id == id })
        override suspend fun update(profile: UserProfileEntity) { lastUpdated = profile }
    }
}
