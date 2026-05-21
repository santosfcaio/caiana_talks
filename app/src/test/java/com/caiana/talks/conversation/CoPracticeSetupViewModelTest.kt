package com.caiana.talks.conversation

import app.cash.turbine.test
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.ui.conversation.CoPracticeSetupViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoPracticeSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: CoPracticeSetupViewModel

    private val profile1 = UserProfileEntity(id = 1, name = "Caio")
    private val profile2 = UserProfileEntity(id = 2, name = "Ana")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        every { userRepository.getAllProfiles() } returns flowOf(listOf(profile1, profile2))
        viewModel = CoPracticeSetupViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `canStart is false initially`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canStart is false when only first profile selected`() = runTest {
        viewModel.onSelectFirst(1)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canStart is false when same profile selected twice`() = runTest {
        viewModel.onSelectFirst(1)
        viewModel.onSelectSecond(1)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canStart is true for two distinct profiles`() = runTest {
        viewModel.onSelectFirst(1)
        viewModel.onSelectSecond(2)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `profile list is loaded`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.profiles.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canStart resets to false when same profile selected for second slot`() = runTest {
        viewModel.onSelectFirst(1)
        viewModel.onSelectSecond(2)
        viewModel.onSelectSecond(1) // change second to same as first
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
