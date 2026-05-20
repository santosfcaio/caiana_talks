package com.caiana.talks.ui

import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.ui.profileselection.ProfileSelectionUiState
import com.caiana.talks.ui.profileselection.ProfileSelectionViewModel
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserRepository
    private lateinit var viewModel: ProfileSelectionViewModel

    private val caioProfile = UserProfileEntity(id = 1, name = "Caio")
    private val anaProfile = UserProfileEntity(id = 2, name = "Ana")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserRepository(allProfiles = listOf(caioProfile, anaProfile))
        viewModel = ProfileSelectionViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- uiState ---

    @Test
    fun `initial uiState is Loading`() {
        // Arrange — viewModel created in setUp

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertTrue(state is ProfileSelectionUiState.Loading)
    }

    @Test
    fun `uiState transitions to ShowSelection once profiles are loaded`() = runTest {
        // Arrange — fakeRepository emits profiles immediately

        // Act & Assert — collect the flow so WhileSubscribed activates it
        viewModel.uiState.test {
            skipItems(1) // skip Loading
            assertTrue(awaitItem() is ProfileSelectionUiState.ShowSelection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ShowSelection contains both profiles`() = runTest {
        // Arrange

        // Act & Assert
        viewModel.uiState.test {
            skipItems(1) // skip Loading
            val state = awaitItem() as ProfileSelectionUiState.ShowSelection
            assertEquals(listOf(caioProfile, anaProfile), state.profiles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- onUserSelected ---

    @Test
    fun `onUserSelected calls repository selectUser with given id`() = runTest {
        // Arrange
        val expectedId = 1

        // Act
        viewModel.onUserSelected(expectedId)
        advanceUntilIdle()

        // Assert
        assertEquals(expectedId, fakeRepository.lastSelectedId)
    }

    @Test
    fun `onUserSelected with id 2 saves Ana to repository`() = runTest {
        // Arrange
        val expectedId = 2

        // Act
        viewModel.onUserSelected(expectedId)
        advanceUntilIdle()

        // Assert
        assertEquals(expectedId, fakeRepository.lastSelectedId)
    }

    // --- clearAndReselect ---

    @Test
    fun `clearAndReselect calls repository clearActiveUser`() = runTest {
        // Arrange

        // Act
        viewModel.clearAndReselect()
        advanceUntilIdle()

        // Assert
        assertTrue(fakeRepository.clearCalled)
    }

    // --- Fake ---

    private class FakeUserRepository(
        private val allProfiles: List<UserProfileEntity> = emptyList(),
        private val activeUser: UserProfileEntity? = null
    ) : UserRepository {
        var lastSelectedId: Int? = null
        var clearCalled = false

        private val activeUserIdFlow = MutableStateFlow(activeUser?.id)

        override fun getAllProfiles(): Flow<List<UserProfileEntity>> = flowOf(allProfiles)

        override fun getActiveUserProfile(): Flow<UserProfileEntity?> =
            flowOf(activeUser)

        override suspend fun selectUser(id: Int) {
            lastSelectedId = id
            activeUserIdFlow.value = id
        }

        override suspend fun clearActiveUser() {
            clearCalled = true
            activeUserIdFlow.value = null
        }

        override suspend fun updateProfile(profile: UserProfileEntity) {}
    }
}
