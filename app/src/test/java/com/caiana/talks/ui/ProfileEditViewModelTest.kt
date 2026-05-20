package com.caiana.talks.ui

import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import com.caiana.talks.ui.profileedit.ProfileEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserRepository
    private lateinit var viewModel: ProfileEditViewModel

    private val caioWithPrefs = UserProfileEntity(
        id = 1,
        name = "Caio",
        learningGoals = "travel",
        preferredThemes = "restaurant,shopping",
        aiVoiceGender = "masculine",
        aiVoiceAccent = "british",
        aiSpeechRate = "fast"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserRepository(activeUser = caioWithPrefs)
        viewModel = ProfileEditViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- initial state ---

    @Test
    fun `initial uiState is loading`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState loads learning goal from entity`() = runTest {
        advanceUntilIdle()
        assertEquals(LearningGoal.TRAVEL, viewModel.uiState.value.learningGoal)
    }

    @Test
    fun `uiState loads selected themes from entity`() = runTest {
        advanceUntilIdle()
        assertEquals(
            setOf(ConversationTheme.RESTAURANT, ConversationTheme.SHOPPING),
            viewModel.uiState.value.selectedThemes
        )
    }

    @Test
    fun `uiState loads voice preferences from entity`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(VoiceGender.MASCULINE, state.voiceGender)
        assertEquals(VoiceAccent.BRITISH, state.voiceAccent)
        assertEquals(SpeechRate.FAST, state.speechRate)
    }

    // --- setLearningGoal ---

    @Test
    fun `setLearningGoal updates uiState`() = runTest {
        advanceUntilIdle()
        viewModel.setLearningGoal(LearningGoal.BUSINESS)
        assertEquals(LearningGoal.BUSINESS, viewModel.uiState.value.learningGoal)
    }

    @Test
    fun `setLearningGoal from TRAVEL to CASUAL updates correctly`() = runTest {
        advanceUntilIdle()
        viewModel.setLearningGoal(LearningGoal.CASUAL)
        assertEquals(LearningGoal.CASUAL, viewModel.uiState.value.learningGoal)
    }

    // --- toggleTheme ---

    @Test
    fun `toggleTheme adds theme when not selected`() = runTest {
        advanceUntilIdle()
        viewModel.toggleTheme(ConversationTheme.AIRPORT)
        assertTrue(ConversationTheme.AIRPORT in viewModel.uiState.value.selectedThemes)
    }

    @Test
    fun `toggleTheme removes theme when already selected`() = runTest {
        advanceUntilIdle()
        viewModel.toggleTheme(ConversationTheme.RESTAURANT) // was in entity
        assertTrue(ConversationTheme.RESTAURANT !in viewModel.uiState.value.selectedThemes)
    }

    @Test
    fun `toggleTheme preserves other selections when adding`() = runTest {
        advanceUntilIdle()
        viewModel.toggleTheme(ConversationTheme.HOTEL)
        val themes = viewModel.uiState.value.selectedThemes
        assertTrue(ConversationTheme.RESTAURANT in themes)
        assertTrue(ConversationTheme.SHOPPING in themes)
        assertTrue(ConversationTheme.HOTEL in themes)
    }

    // --- setVoiceGender ---

    @Test
    fun `setVoiceGender updates uiState`() = runTest {
        advanceUntilIdle()
        viewModel.setVoiceGender(VoiceGender.FEMININE)
        assertEquals(VoiceGender.FEMININE, viewModel.uiState.value.voiceGender)
    }

    // --- setVoiceAccent ---

    @Test
    fun `setVoiceAccent updates uiState`() = runTest {
        advanceUntilIdle()
        viewModel.setVoiceAccent(VoiceAccent.AMERICAN)
        assertEquals(VoiceAccent.AMERICAN, viewModel.uiState.value.voiceAccent)
    }

    // --- setSpeechRate ---

    @Test
    fun `setSpeechRate updates uiState`() = runTest {
        advanceUntilIdle()
        viewModel.setSpeechRate(SpeechRate.SLOW)
        assertEquals(SpeechRate.SLOW, viewModel.uiState.value.speechRate)
    }

    // --- savePreferences ---

    @Test
    fun `savePreferences sets isSaved to true`() = runTest {
        advanceUntilIdle()
        viewModel.savePreferences()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `savePreferences calls repository updateProfile`() = runTest {
        advanceUntilIdle()
        viewModel.savePreferences()
        advanceUntilIdle()
        assertNotNull(fakeRepository.lastUpdatedProfile)
    }

    @Test
    fun `savePreferences persists changed learning goal`() = runTest {
        advanceUntilIdle()
        viewModel.setLearningGoal(LearningGoal.CASUAL)
        viewModel.savePreferences()
        advanceUntilIdle()
        assertEquals("casual", fakeRepository.lastUpdatedProfile?.learningGoals)
    }

    @Test
    fun `savePreferences persists selected themes as CSV`() = runTest {
        advanceUntilIdle()
        // initial themes: restaurant, shopping → add hotel
        viewModel.toggleTheme(ConversationTheme.HOTEL)
        viewModel.savePreferences()
        advanceUntilIdle()
        val saved = fakeRepository.lastUpdatedProfile?.preferredThemes ?: ""
        val tokens = saved.split(",").toSet()
        assertTrue("restaurant" in tokens)
        assertTrue("shopping" in tokens)
        assertTrue("hotel" in tokens)
    }

    @Test
    fun `savePreferences persists voice preferences`() = runTest {
        advanceUntilIdle()
        viewModel.setVoiceGender(VoiceGender.FEMININE)
        viewModel.setVoiceAccent(VoiceAccent.AMERICAN)
        viewModel.setSpeechRate(SpeechRate.SLOW)
        viewModel.savePreferences()
        advanceUntilIdle()
        val saved = fakeRepository.lastUpdatedProfile
        assertEquals("feminine", saved?.aiVoiceGender)
        assertEquals("american", saved?.aiVoiceAccent)
        assertEquals("slow", saved?.aiSpeechRate)
    }

    // --- Fake ---

    private class FakeUserRepository(
        private val activeUser: UserProfileEntity? = null
    ) : UserRepository {
        var lastUpdatedProfile: UserProfileEntity? = null

        override fun getAllProfiles(): Flow<List<UserProfileEntity>> = flowOf(emptyList())
        override fun getActiveUserProfile(): Flow<UserProfileEntity?> = flowOf(activeUser)
        override suspend fun selectUser(id: Int) {}
        override suspend fun clearActiveUser() {}
        override suspend fun updateProfile(profile: UserProfileEntity) {
            lastUpdatedProfile = profile
        }
    }
}
