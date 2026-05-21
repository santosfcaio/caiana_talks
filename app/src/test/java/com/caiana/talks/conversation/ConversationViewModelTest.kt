package com.caiana.talks.conversation

import androidx.lifecycle.SavedStateHandle
import com.caiana.talks.data.conversation.SpeechRecognizerService
import com.caiana.talks.data.conversation.SttEvent
import com.caiana.talks.data.conversation.SystemPromptBuilder
import com.caiana.talks.data.conversation.TextToSpeechService
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.remote.ConversationAiClient
import com.caiana.talks.data.remote.SystemPrompt
import com.caiana.talks.data.repository.ConversationRepository
import com.caiana.talks.data.repository.SessionHandle
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationError
import com.caiana.talks.domain.model.ConversationSessionSummary
import com.caiana.talks.domain.model.SessionResult
import com.caiana.talks.domain.model.SessionStatus
import com.caiana.talks.ui.conversation.ConversationUiState
import com.caiana.talks.ui.conversation.ConversationViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sttService: SpeechRecognizerService
    private lateinit var ttsService: TextToSpeechService
    private lateinit var aiClient: ConversationAiClient
    private lateinit var repository: ConversationRepository
    private lateinit var promptBuilder: SystemPromptBuilder
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ConversationViewModel

    // masculine + american → AiPersona.MICHAEL (displayName = "Michael")
    private val profile = UserProfileEntity(
        id = 1, name = "Caio",
        aiVoiceGender = "masculine", aiVoiceAccent = "american"
    )

    private val fakeHandle = SessionHandle(sessionId = 1, groupId = null)
    private val fakePrompt = SystemPrompt(staticBlock = "static", personalizationBlock = "personal")
    private val emptyMeta = AiResponseMeta(emptyList(), emptyList(), false)
    private val savedResult = SessionResult(
        outcome = SessionResult.Outcome.SAVED,
        summaries = listOf(
            ConversationSessionSummary(1, 1, "Caio", 0L, 90_000L, 0, emptyList(), null)
        )
    )
    private val discardedResult = SessionResult(
        outcome = SessionResult.Outcome.DISCARDED_TOO_SHORT,
        summaries = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sttService = mockk(relaxed = true)
        ttsService = mockk(relaxed = true)
        aiClient = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        promptBuilder = mockk()
        userRepository = mockk()

        every { userRepository.getAllProfiles() } returns flowOf(listOf(profile))
        every { userRepository.getActiveUserProfile() } returns flowOf(profile)
        every { promptBuilder.build(any()) } returns fakePrompt
        every { ttsService.isSpeaking } returns MutableStateFlow(false)
        coEvery { repository.startSession(any()) } returns fakeHandle

        // With UnconfinedTestDispatcher, the ViewModel's init coroutine runs IMMEDIATELY,
        // so config and aiPersonaName are already set by the time setUp() returns.
        viewModel = ConversationViewModel(
            savedStateHandle = SavedStateHandle(),
            sttService = sttService,
            ttsService = ttsService,
            aiClient = aiClient,
            repository = repository,
            promptBuilder = promptBuilder,
            userRepository = userRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is IDLE`() = runTest {
        assertEquals(ConversationUiState.Phase.IDLE, viewModel.uiState.value.phase)
    }

    @Test
    fun `persona name is set from config after init`() = runTest {
        // Init already completed eagerly in setUp()
        assertEquals("Michael", viewModel.uiState.value.aiPersonaName)
    }

    @Test
    fun `onStart transitions to LISTENING`() = runTest {
        every { sttService.listen(any()) } returns flowOf()
        viewModel.onStart()
        assertEquals(ConversationUiState.Phase.LISTENING, viewModel.uiState.value.phase)
    }

    @Test
    fun `STT Final event triggers THINKING phase`() = runTest(testDispatcher) {
        // Use returnsMany to prevent infinite loop: 2nd listen call returns empty flow
        every { sttService.listen(any()) } returnsMany listOf(
            flowOf(SttEvent.Final("Hello world")),
            flowOf()
        )
        every { aiClient.streamReply(any(), any(), any()) } returns flowOf(
            AiStreamEvent.Completed(emptyMeta)
        )

        val observedPhases = mutableListOf<ConversationUiState.Phase>()
        val collectJob = launch { viewModel.uiState.collect { observedPhases.add(it.phase) } }

        viewModel.onStart()
        collectJob.cancel()

        assertTrue("Expected THINKING in: $observedPhases",
            observedPhases.contains(ConversationUiState.Phase.THINKING))
    }

    @Test
    fun `onPermissionDenied sets error MIC_PERMISSION_DENIED`() = runTest {
        viewModel.onPermissionDenied()
        assertEquals(ConversationError.MIC_PERMISSION_DENIED, viewModel.uiState.value.error)
    }

    @Test
    fun `onRetry clears error and resumes LISTENING`() = runTest {
        every { sttService.listen(any()) } returns flowOf()
        viewModel.onPermissionDenied()
        viewModel.onRetry()
        assertNull(viewModel.uiState.value.error)
        assertEquals(ConversationUiState.Phase.LISTENING, viewModel.uiState.value.phase)
    }

    @Test
    fun `Silence STT event stays in LISTENING with empty transcript`() = runTest {
        every { sttService.listen(any()) } returns flowOf(SttEvent.Silence)
        viewModel.onStart()
        assertEquals(ConversationUiState.Phase.LISTENING, viewModel.uiState.value.phase)
        assertEquals("", viewModel.uiState.value.liveTranscript)
    }

    @Test
    fun `AI API_ERROR sets error state`() = runTest {
        every { sttService.listen(any()) } returnsMany listOf(
            flowOf(SttEvent.Final("hello")),
            flowOf()
        )
        every { aiClient.streamReply(any(), any(), any()) } returns flowOf(
            AiStreamEvent.Failed(ConversationError.AI_API_ERROR)
        )

        viewModel.onStart()

        assertEquals(ConversationError.AI_API_ERROR, viewModel.uiState.value.error)
    }

    @Test
    fun `meta pt true sets portugueseNudgeVisible`() = runTest {
        val ptMeta = AiResponseMeta(emptyList(), emptyList(), userSpokePortuguese = true)
        every { sttService.listen(any()) } returnsMany listOf(
            flowOf(SttEvent.Final("oi")),
            flowOf()
        )
        every { aiClient.streamReply(any(), any(), any()) } returns flowOf(
            AiStreamEvent.Completed(ptMeta)
        )

        viewModel.onStart()

        assertTrue(viewModel.uiState.value.portugueseNudgeVisible)
    }

    @Test
    fun `onEndSession finalizes session as COMPLETED`() = runTest {
        coEvery { repository.finalizeSession(any(), any()) } returns savedResult
        every { sttService.listen(any()) } returns flowOf()

        viewModel.onStart()
        viewModel.onEndSession()

        coVerify { repository.finalizeSession(fakeHandle, SessionStatus.COMPLETED) }
    }

    @Test
    fun `onEndSession with DISCARDED result calls finalizeSession COMPLETED`() = runTest {
        coEvery { repository.finalizeSession(any(), any()) } returns discardedResult
        every { sttService.listen(any()) } returns flowOf()

        viewModel.onStart()
        viewModel.onEndSession()

        coVerify { repository.finalizeSession(any(), SessionStatus.COMPLETED) }
    }

    @Test
    fun `onAppBackgrounded finalizes as PARTIAL`() = runTest {
        coEvery { repository.finalizeSession(any(), any()) } returns savedResult
        every { sttService.listen(any()) } returns flowOf()

        viewModel.onStart()
        viewModel.onAppBackgrounded()

        coVerify { repository.finalizeSession(fakeHandle, SessionStatus.PARTIAL) }
    }
}
