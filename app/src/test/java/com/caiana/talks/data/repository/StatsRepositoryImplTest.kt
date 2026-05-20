package com.caiana.talks.data.repository

import app.cash.turbine.test
import com.caiana.talks.data.local.db.CategoryCount
import com.caiana.talks.data.local.db.CorrectionDao
import com.caiana.talks.data.local.db.CorrectionEntity
import com.caiana.talks.data.local.db.SessionDao
import com.caiana.talks.data.local.db.SessionEntity
import com.caiana.talks.domain.model.CefrLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryImplTest {

    private lateinit var fakeSessionDao: FakeSessionDao
    private lateinit var fakeCorrectionDao: FakeCorrectionDao
    private lateinit var repository: StatsRepositoryImpl

    @Before
    fun setUp() {
        fakeSessionDao = FakeSessionDao()
        fakeCorrectionDao = FakeCorrectionDao()
        repository = StatsRepositoryImpl(fakeSessionDao, fakeCorrectionDao)
    }

    // ── computeCefrLevel ──────────────────────────────────────────────────────

    @Test
    fun `computeCefrLevel returns null when sessionCount is 0`() {
        assertNull(repository.computeCefrLevel(0, 0))
    }

    @Test
    fun `computeCefrLevel returns A1 when sessionCount is 1`() {
        assertEquals(CefrLevel.A1, repository.computeCefrLevel(1, 20))
    }

    @Test
    fun `computeCefrLevel returns A1 when sessionCount is 2 (less than 3)`() {
        assertEquals(CefrLevel.A1, repository.computeCefrLevel(2, 5))
    }

    @Test
    fun `computeCefrLevel returns A1 when avg errors greater than 15`() {
        // 3 sessions, 48 errors → avg = 16 > 15
        assertEquals(CefrLevel.A1, repository.computeCefrLevel(3, 48))
    }

    @Test
    fun `computeCefrLevel returns A2 for 4 sessions with avg 12`() {
        // sessionCount in 3..5 AND avg in (10, 15]
        assertEquals(CefrLevel.A2, repository.computeCefrLevel(4, 48))
    }

    @Test
    fun `computeCefrLevel returns B1 for 7 sessions with avg 8`() {
        // sessionCount in 6..10 AND avg in (5, 10]
        assertEquals(CefrLevel.B1, repository.computeCefrLevel(7, 56))
    }

    @Test
    fun `computeCefrLevel returns B2 for 15 sessions with avg 4`() {
        // sessionCount in 11..20 AND avg in (2, 5]
        assertEquals(CefrLevel.B2, repository.computeCefrLevel(15, 60))
    }

    @Test
    fun `computeCefrLevel returns C1 for 25 sessions with avg 1_2`() {
        // sessionCount > 20 AND avg in [1, 2]
        assertEquals(CefrLevel.C1, repository.computeCefrLevel(25, 30))
    }

    @Test
    fun `computeCefrLevel returns C2 for 35 sessions with avg under 1`() {
        // sessionCount > 30 AND avg < 1
        assertEquals(CefrLevel.C2, repository.computeCefrLevel(35, 28))
    }

    @Test
    fun `computeCefrLevel returns B1 fallback for unmatched combination`() {
        // 25 sessions with avg 8 — doesn't fit any specific bucket cleanly
        assertEquals(CefrLevel.B1, repository.computeCefrLevel(25, 200))
    }

    // ── computeInsights ───────────────────────────────────────────────────────

    @Test
    fun `computeInsights returns early-exit message when sessionCount is less than 2`() {
        val result = repository.computeInsights(1, 0, 0, 0, 0, emptyList())
        assertEquals(listOf("Complete mais sessões para desbloquear insights."), result)
    }

    @Test
    fun `computeInsights returns grammar insight when grammar over 50 percent of errors`() {
        // 2 sessions, 10 grammar, 2 vocab, 2 fluency → grammar = 71%
        val result = repository.computeInsights(2, 10, 2, 2, 14, emptyList())
        assertEquals(1, result.size)
        assertEquals(
            "Você comete mais erros de gramática. Foque nos tempos verbais e estrutura das frases.",
            result[0]
        )
    }

    @Test
    fun `computeInsights returns vocabulary insight when vocab over 50 percent of errors`() {
        val result = repository.computeInsights(2, 2, 10, 2, 14, emptyList())
        assertEquals(1, result.size)
        assertEquals(
            "Expandir seu vocabulário é sua maior área de crescimento. Leia e ouça em inglês diariamente.",
            result[0]
        )
    }

    @Test
    fun `computeInsights returns fluency insight when fluency over 50 percent of errors`() {
        val result = repository.computeInsights(2, 2, 2, 10, 14, emptyList())
        assertEquals(1, result.size)
        assertEquals(
            "Seu principal desafio é a fluência. Tente falar de forma mais contínua, sem pausas longas.",
            result[0]
        )
    }

    @Test
    fun `computeInsights returns default insight when no corrections and 2 sessions`() {
        val result = repository.computeInsights(2, 0, 0, 0, 0, emptyList())
        assertEquals(listOf("Continue praticando! Cada sessão te aproxima da fluência."), result)
    }

    @Test
    fun `computeInsights returns repeated-error insight when same category dominates last 3 sessions`() {
        val sessions = listOf(
            makeSession(id = 1) to listOf(
                makeCorrectionEntity(1, "GRAMMAR"), makeCorrectionEntity(1, "GRAMMAR")
            ),
            makeSession(id = 2) to listOf(
                makeCorrectionEntity(2, "GRAMMAR"), makeCorrectionEntity(2, "FLUENCY")
            ),
            makeSession(id = 3) to listOf(
                makeCorrectionEntity(3, "GRAMMAR")
            )
        )
        val result = repository.computeInsights(3, 5, 0, 1, 6, sessions)
        val repeatedInsight = result.firstOrNull {
            it.contains("Gramática") && it.contains("prática dedicada")
        }
        assert(repeatedInsight != null) { "Expected repeated-error insight but got: $result" }
    }

    @Test
    fun `computeInsights returns progress insight when error rate is decreasing`() {
        // 6 sessions: newest 3 have 1 correction each, oldest 3 have 5 corrections each
        val recentSessions = (1..3).map { i ->
            makeSession(id = i) to listOf(makeCorrectionEntity(i, "GRAMMAR"))
        }
        val oldSessions = (4..6).map { i ->
            makeSession(id = i) to (1..5).map { makeCorrectionEntity(i, "GRAMMAR") }
        }
        val sessionDataList = recentSessions + oldSessions
        val result = repository.computeInsights(6, 18, 0, 0, 18, sessionDataList)
        val progressInsight = result.firstOrNull { it.contains("diminuindo") }
        assert(progressInsight != null) { "Expected progress insight but got: $result" }
    }

    @Test
    fun `computeInsights caps result at 3 insights`() {
        // Set up conditions that could trigger multiple insights
        // grammar dominates + 5 sessions with improving rate + repeated errors
        val recentSessions = (1..3).map { i ->
            makeSession(id = i) to listOf(makeCorrectionEntity(i, "GRAMMAR"))
        }
        val oldSessions = (4..5).map { i ->
            makeSession(id = i) to (1..5).map { makeCorrectionEntity(i, "GRAMMAR") }
        }
        val sessionDataList = recentSessions + oldSessions
        val result = repository.computeInsights(5, 100, 0, 0, 100, sessionDataList)
        assert(result.size <= 3) { "Expected at most 3 insights but got ${result.size}" }
    }

    // ── getProgressSnapshot (flow) ─────────────────────────────────────────────

    @Test
    fun `getProgressSnapshot emits empty snapshot when no sessions`() = runTest {
        fakeSessionDao.setSessionsForProfile(1, emptyList())
        fakeCorrectionDao.setCategoryCountsForProfile(1, emptyList())

        repository.getProgressSnapshot(1).test {
            val snapshot = awaitItem()
            assertNull(snapshot.cefrLevel)
            assertEquals(0, snapshot.grammarErrors)
            assertEquals(0, snapshot.vocabularyErrors)
            assertEquals(0, snapshot.fluencyErrors)
            assertEquals(emptyList<Any>(), snapshot.sessions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProgressSnapshot emits snapshot with CEFR level when sessions exist`() = runTest {
        val session = makeSession(id = 1, profileId = 1, startedAt = 1000L, endedAt = 61_000L)
        fakeSessionDao.setSessionsForProfile(1, listOf(session))
        fakeCorrectionDao.setCorrectionsForSession(1, listOf(
            makeCorrectionEntity(1, "GRAMMAR", description = "used 'make'")
        ))
        fakeCorrectionDao.setCategoryCountsForProfile(1, listOf(
            CategoryCount("GRAMMAR", 1)
        ))

        repository.getProgressSnapshot(1).test {
            val snapshot = awaitItem()
            assertEquals(CefrLevel.A1, snapshot.cefrLevel)
            assertEquals(1, snapshot.grammarErrors)
            assertEquals(1, snapshot.sessions.size)
            assertEquals(1, snapshot.sessions[0].durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProgressSnapshot caps corrections at 5 per session`() = runTest {
        val session = makeSession(id = 1, profileId = 1)
        fakeSessionDao.setSessionsForProfile(1, listOf(session))
        val corrections = (1..8).map { makeCorrectionEntity(1, "GRAMMAR", "error $it") }
        fakeCorrectionDao.setCorrectionsForSession(1, corrections)
        fakeCorrectionDao.setCategoryCountsForProfile(1, listOf(CategoryCount("GRAMMAR", 8)))

        repository.getProgressSnapshot(1).test {
            val snapshot = awaitItem()
            assertEquals(5, snapshot.sessions[0].corrections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProgressSnapshot formats duration correctly for less than 60 minutes`() = runTest {
        val session = makeSession(id = 1, profileId = 1, startedAt = 0L, endedAt = 15 * 60_000L)
        fakeSessionDao.setSessionsForProfile(1, listOf(session))
        fakeCorrectionDao.setCorrectionsForSession(1, emptyList())
        fakeCorrectionDao.setCategoryCountsForProfile(1, emptyList())

        repository.getProgressSnapshot(1).test {
            val snapshot = awaitItem()
            assertEquals(15, snapshot.sessions[0].durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProgressSnapshot formats duration correctly for 60 minutes or more`() = runTest {
        val session = makeSession(id = 1, profileId = 1, startedAt = 0L, endedAt = 75 * 60_000L)
        fakeSessionDao.setSessionsForProfile(1, listOf(session))
        fakeCorrectionDao.setCorrectionsForSession(1, emptyList())
        fakeCorrectionDao.setCategoryCountsForProfile(1, emptyList())

        repository.getProgressSnapshot(1).test {
            val snapshot = awaitItem()
            assertEquals(75, snapshot.sessions[0].durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeSession(
        id: Int = 1,
        profileId: Int = 1,
        startedAt: Long = 0L,
        endedAt: Long = 60_000L
    ) = SessionEntity(id = id, userProfileId = profileId, startedAt = startedAt, endedAt = endedAt)

    private fun makeCorrectionEntity(
        sessionId: Int,
        category: String,
        description: String = "test error",
        timestamp: Long = 0L
    ) = CorrectionEntity(
        sessionId = sessionId,
        category = category,
        description = description,
        timestamp = timestamp
    )

    // ── Fake DAOs ─────────────────────────────────────────────────────────────

    private class FakeSessionDao : SessionDao {
        private val sessionsMap = mutableMapOf<Int, MutableStateFlow<List<SessionEntity>>>()

        fun setSessionsForProfile(profileId: Int, sessions: List<SessionEntity>) {
            sessionsMap.getOrPut(profileId) { MutableStateFlow(emptyList()) }.value = sessions
        }

        override fun getSessionsForProfile(profileId: Int): Flow<List<SessionEntity>> =
            sessionsMap.getOrPut(profileId) { MutableStateFlow(emptyList()) }

        override suspend fun insertSession(session: SessionEntity): Long = 0L

        override fun getSessionCount(profileId: Int): Flow<Int> = flowOf(0)
    }

    private class FakeCorrectionDao : CorrectionDao {
        private val correctionsBySession = mutableMapOf<Int, MutableStateFlow<List<CorrectionEntity>>>()
        private val categoryCountsByProfile = mutableMapOf<Int, MutableStateFlow<List<CategoryCount>>>()

        fun setCorrectionsForSession(sessionId: Int, corrections: List<CorrectionEntity>) {
            correctionsBySession.getOrPut(sessionId) { MutableStateFlow(emptyList()) }.value = corrections
        }

        fun setCategoryCountsForProfile(profileId: Int, counts: List<CategoryCount>) {
            categoryCountsByProfile.getOrPut(profileId) { MutableStateFlow(emptyList()) }.value = counts
        }

        override fun getCorrectionsForSession(sessionId: Int): Flow<List<CorrectionEntity>> =
            correctionsBySession.getOrPut(sessionId) { MutableStateFlow(emptyList()) }

        override fun getCategoryCountsForProfile(profileId: Int): Flow<List<CategoryCount>> =
            categoryCountsByProfile.getOrPut(profileId) { MutableStateFlow(emptyList()) }

        override suspend fun insertCorrection(correction: CorrectionEntity) {}
    }
}
