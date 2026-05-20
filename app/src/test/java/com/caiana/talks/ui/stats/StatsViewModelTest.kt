package com.caiana.talks.ui.stats

import app.cash.turbine.test
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.StatsRepository
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.CefrLevel
import com.caiana.talks.domain.model.CorrectionCategory
import com.caiana.talks.domain.model.CorrectionSummary
import com.caiana.talks.domain.model.ProgressSnapshot
import com.caiana.talks.domain.model.SessionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeStatsRepository: FakeStatsRepository
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var viewModel: StatsViewModel

    private val caioProfile = UserProfileEntity(id = 1, name = "Caio")
    private val anaProfile = UserProfileEntity(id = 2, name = "Ana")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial uiState is loading`() {
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository()
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    // ── transitions ───────────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to content once snapshot is emitted`() = runTest {
        val snapshot = makeSnapshot(cefrLevel = CefrLevel.A1, grammarErrors = 3)
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository(snapshotForProfile = mapOf(1 to snapshot))
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1) // skip initial Loading
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(CefrLevel.A1, state.cefrLevel)
            assertEquals(3, state.grammarErrors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState shows null cefrLevel when snapshot has no sessions`() = runTest {
        val snapshot = makeSnapshot(cefrLevel = null)
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository(snapshotForProfile = mapOf(1 to snapshot))
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertNull(state.cefrLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects sessions from snapshot`() = runTest {
        val sessionSummary = SessionSummary(
            id = 1,
            date = 1000L,
            durationMinutes = 15,
            corrections = listOf(
                CorrectionSummary(CorrectionCategory.GRAMMAR, "used 'make' instead of 'do'")
            )
        )
        val snapshot = makeSnapshot(sessions = listOf(sessionSummary))
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository(snapshotForProfile = mapOf(1 to snapshot))
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertEquals(1, state.sessions.size)
            assertEquals(15, state.sessions[0].durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects insights from snapshot`() = runTest {
        val insights = listOf("Você comete mais erros de gramática.")
        val snapshot = makeSnapshot(insights = insights)
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository(snapshotForProfile = mapOf(1 to snapshot))
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertEquals(insights, state.insights)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── profile switch ────────────────────────────────────────────────────────

    @Test
    fun `uiState updates when active profile changes`() = runTest {
        val caioSnapshot = makeSnapshot(cefrLevel = CefrLevel.B1, grammarErrors = 10)
        val anaSnapshot = makeSnapshot(cefrLevel = CefrLevel.A2, grammarErrors = 5)
        val activeProfileFlow = MutableStateFlow<UserProfileEntity?>(caioProfile)
        fakeUserRepository = FakeUserRepository(activeUserFlow = activeProfileFlow)
        fakeStatsRepository = FakeStatsRepository(
            snapshotForProfile = mapOf(1 to caioSnapshot, 2 to anaSnapshot)
        )
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1) // Loading
            val caioState = awaitItem()
            assertEquals(CefrLevel.B1, caioState.cefrLevel)
            assertEquals(10, caioState.grammarErrors)

            // Switch to Ana
            activeProfileFlow.value = anaProfile
            val anaState = awaitItem()
            assertEquals(CefrLevel.A2, anaState.cefrLevel)
            assertEquals(5, anaState.grammarErrors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState shows not-loading empty state when no active profile`() = runTest {
        fakeUserRepository = FakeUserRepository(activeUser = null)
        fakeStatsRepository = FakeStatsRepository()
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1) // Loading
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.cefrLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── error counts ──────────────────────────────────────────────────────────

    @Test
    fun `uiState exposes all error category counts from snapshot`() = runTest {
        val snapshot = makeSnapshot(grammarErrors = 5, vocabularyErrors = 3, fluencyErrors = 2)
        fakeUserRepository = FakeUserRepository(activeUser = caioProfile)
        fakeStatsRepository = FakeStatsRepository(snapshotForProfile = mapOf(1 to snapshot))
        viewModel = StatsViewModel(fakeStatsRepository, fakeUserRepository)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertEquals(5, state.grammarErrors)
            assertEquals(3, state.vocabularyErrors)
            assertEquals(2, state.fluencyErrors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeSnapshot(
        cefrLevel: CefrLevel? = null,
        grammarErrors: Int = 0,
        vocabularyErrors: Int = 0,
        fluencyErrors: Int = 0,
        sessions: List<SessionSummary> = emptyList(),
        insights: List<String> = emptyList()
    ) = ProgressSnapshot(
        cefrLevel = cefrLevel,
        grammarErrors = grammarErrors,
        vocabularyErrors = vocabularyErrors,
        fluencyErrors = fluencyErrors,
        sessions = sessions,
        insights = insights
    )

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeStatsRepository(
        private val snapshotForProfile: Map<Int, ProgressSnapshot> = emptyMap()
    ) : StatsRepository {
        private val defaultSnapshot = ProgressSnapshot(
            cefrLevel = null,
            grammarErrors = 0,
            vocabularyErrors = 0,
            fluencyErrors = 0,
            sessions = emptyList(),
            insights = emptyList()
        )

        override fun getProgressSnapshot(profileId: Int): Flow<ProgressSnapshot> =
            flowOf(snapshotForProfile[profileId] ?: defaultSnapshot)
    }

    private class FakeUserRepository(
        private val activeUser: UserProfileEntity? = null,
        private val activeUserFlow: MutableStateFlow<UserProfileEntity?> = MutableStateFlow(activeUser)
    ) : UserRepository {
        override fun getAllProfiles(): Flow<List<UserProfileEntity>> = flowOf(emptyList())
        override fun getActiveUserProfile(): Flow<UserProfileEntity?> = activeUserFlow
        override suspend fun selectUser(id: Int) {}
        override suspend fun clearActiveUser() { activeUserFlow.value = null }
        override suspend fun updateProfile(profile: UserProfileEntity) {}
    }
}
