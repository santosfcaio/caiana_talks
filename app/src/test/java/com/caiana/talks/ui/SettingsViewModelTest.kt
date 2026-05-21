package com.caiana.talks.ui

import app.cash.turbine.test
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import com.caiana.talks.ui.settings.SettingsViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userPrefs: UserPreferencesDataStore
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userPrefs = mockk(relaxed = true)
        every { userPrefs.openrouterApiKey } returns flowOf("stored-key")
        every { userPrefs.aiModel } returns flowOf("stored-model")
        viewModel = SettingsViewModel(userPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `apiKeyOverride emits value from DataStore`() = runTest {
        viewModel.apiKeyOverride.test {
            assertEquals("", awaitItem()) // initial state before upstream collects
            assertEquals("stored-key", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `modelOverride emits value from DataStore`() = runTest {
        viewModel.modelOverride.test {
            assertEquals("", awaitItem())
            assertEquals("stored-model", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setApiKey calls setOpenrouterApiKey on DataStore`() = runTest {
        viewModel.setApiKey("new-key")
        advanceUntilIdle()
        coVerify { userPrefs.setOpenrouterApiKey("new-key") }
    }

    @Test
    fun `setModel calls setAiModel on DataStore`() = runTest {
        viewModel.setModel("new-model")
        advanceUntilIdle()
        coVerify { userPrefs.setAiModel("new-model") }
    }
}
