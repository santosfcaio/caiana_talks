package com.caiana.talks.conversation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.caiana.talks.data.repository.ConversationRepository
import com.caiana.talks.domain.model.ConversationSessionSummary
import com.caiana.talks.ui.conversation.SessionSummaryViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ConversationRepository
    private val now = System.currentTimeMillis()

    private val singleSummary = ConversationSessionSummary(
        sessionId = 1,
        profileId = 1,
        profileName = "Caio",
        startedAt = now - 90_000L,
        endedAt = now,
        correctionCount = 3,
        vocabulary = listOf("apple", "banana"),
        coPracticeGroupId = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `single mode loads one participant summary`() = runTest {
        coEvery { repository.getSessionSummaryById(1) } returns singleSummary

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "1")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.perParticipant.size)
            assertEquals("Caio", state.perParticipant[0].profileName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single mode duration label under 60 min shows Xmin format`() = runTest {
        val shortSession = singleSummary.copy(
            startedAt = now - 90_000L,
            endedAt = now
        ) // 90 seconds = 1 min
        coEvery { repository.getSessionSummaryById(1) } returns shortSession

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "1")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            val label = state.perParticipant.firstOrNull()?.durationLabel ?: ""
            assertFalse(state.isLoading)
            assert(label.contains("min")) { "Expected 'min' in duration label but got: $label" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duration label for 90 minutes uses Xh Ymin format`() = runTest {
        val longSession = singleSummary.copy(
            startedAt = now - (90 * 60_000L),
            endedAt = now
        )
        coEvery { repository.getSessionSummaryById(1) } returns longSession

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "1")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            val label = state.perParticipant.firstOrNull()?.durationLabel ?: ""
            assert(label.contains("h")) { "Expected 'h' in duration label for 90min but got: $label" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `vocabulary highlights surfaced per participant`() = runTest {
        coEvery { repository.getSessionSummaryById(1) } returns singleSummary

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "1")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("apple", "banana"), state.perParticipant[0].vocabularyHighlights)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dual mode loads two participant summaries by groupId`() = runTest {
        val summary1 = singleSummary.copy(coPracticeGroupId = "group-abc")
        val summary2 = singleSummary.copy(
            sessionId = 2, profileId = 2, profileName = "Ana", coPracticeGroupId = "group-abc"
        )
        coEvery { repository.getSessionSummariesByGroup("group-abc") } returns listOf(summary1, summary2)

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("groupId" to "group-abc")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.perParticipant.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `correction count surfaced per participant`() = runTest {
        coEvery { repository.getSessionSummaryById(1) } returns singleSummary

        val vm = SessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "1")),
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.perParticipant[0].correctionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
