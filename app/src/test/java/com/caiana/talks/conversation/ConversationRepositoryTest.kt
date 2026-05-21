package com.caiana.talks.conversation

import com.caiana.talks.data.local.db.ConversationTurnDao
import com.caiana.talks.data.local.db.ConversationTurnEntity
import com.caiana.talks.data.local.db.CorrectionDao
import com.caiana.talks.data.local.db.CorrectionEntity
import com.caiana.talks.data.local.db.SessionDao
import com.caiana.talks.data.local.db.SessionEntity
import com.caiana.talks.data.local.db.UserProfileDao
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.ConversationRepositoryImpl
import com.caiana.talks.domain.model.AiPersona
import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.CorrectionCategory
import com.caiana.talks.domain.model.DetectedCorrection
import com.caiana.talks.domain.model.ParticipantInfo
import com.caiana.talks.domain.model.SessionMode
import com.caiana.talks.domain.model.SessionResult
import com.caiana.talks.domain.model.SessionStatus
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import com.caiana.talks.domain.model.VoicePreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationRepositoryTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var turnDao: ConversationTurnDao
    private lateinit var correctionDao: CorrectionDao
    private lateinit var profileDao: UserProfileDao
    private lateinit var repo: ConversationRepositoryImpl

    private val singleConfig = ConversationConfig(
        mode = SessionMode.SINGLE,
        participants = listOf(ParticipantInfo(1, "Caio")),
        learningGoal = null,
        themes = emptySet(),
        voice = VoicePreference(VoiceGender.MASCULINE, VoiceAccent.AMERICAN, SpeechRate.NORMAL),
        persona = AiPersona.MICHAEL,
        cefrHint = null
    )

    private val profileEntity = UserProfileEntity(
        id = 1,
        name = "Caio",
        learningGoals = "",
        preferredThemes = "",
        aiVoiceGender = "masculine",
        aiVoiceAccent = "american",
        aiSpeechRate = "normal"
    )

    @Before
    fun setUp() {
        sessionDao = mockk(relaxed = true)
        turnDao = mockk(relaxed = true)
        correctionDao = mockk(relaxed = true)
        profileDao = mockk(relaxed = true)
        repo = ConversationRepositoryImpl(sessionDao, turnDao, correctionDao, profileDao)
    }

    @Test
    fun `startSession single creates one active session row`() = runTest {
        val sessionSlot = slot<SessionEntity>()
        coEvery { sessionDao.insertSession(capture(sessionSlot)) } returns 42L

        repo.startSession(singleConfig)

        val inserted = sessionSlot.captured
        assertEquals(1, inserted.userProfileId)
        assertEquals("active", inserted.status)
        assertEquals("single", inserted.mode)
    }

    @Test
    fun `appendTurn writes ConversationTurnEntity to correct session`() = runTest {
        coEvery { sessionDao.insertSession(any()) } returns 10L
        val turnSlot = slot<ConversationTurnEntity>()
        coEvery { turnDao.insertTurn(capture(turnSlot)) } returns 1L
        coEvery { sessionDao.getSessionById(10) } returns SessionEntity(
            id = 10,
            userProfileId = 1,
            startedAt = 1000L,
            endedAt = 1000L,
            vocabulary = ""
        )

        val handle = repo.startSession(singleConfig)
        val meta = AiResponseMeta(
            corrections = listOf(DetectedCorrection(CorrectionCategory.GRAMMAR, "use past tense")),
            vocabulary = listOf("tense"),
            userSpokePortuguese = false
        )
        repo.appendTurn(handle, 1, "I go yesterday", "I went yesterday.", meta)

        val turn = turnSlot.captured
        assertEquals(10, turn.sessionId)
        assertEquals(1, turn.speakerProfileId)
        assertEquals("I go yesterday", turn.userText)
        assertEquals("I went yesterday.", turn.aiText)
    }

    @Test
    fun `appendTurn inserts CorrectionEntity per correction`() = runTest {
        coEvery { sessionDao.insertSession(any()) } returns 10L
        coEvery { turnDao.insertTurn(any()) } returns 1L
        val correctionSlot = slot<CorrectionEntity>()
        coEvery { correctionDao.insertCorrection(capture(correctionSlot)) } returns Unit
        coEvery { sessionDao.getSessionById(10) } returns SessionEntity(
            id = 10,
            userProfileId = 1,
            startedAt = 1000L,
            endedAt = 1000L,
            vocabulary = ""
        )

        val handle = repo.startSession(singleConfig)
        val meta = AiResponseMeta(
            corrections = listOf(DetectedCorrection(CorrectionCategory.VOCABULARY, "wrong word")),
            vocabulary = emptyList(),
            userSpokePortuguese = false
        )
        repo.appendTurn(handle, 1, "user text", "ai text", meta)

        coVerify { correctionDao.insertCorrection(any()) }
        assertEquals("VOCABULARY", correctionSlot.captured.category)
        assertEquals("wrong word", correctionSlot.captured.description)
    }

    @Test
    fun `finalizeSession less than 60s deletes session and returns DISCARDED`() = runTest {
        val now = System.currentTimeMillis()
        coEvery { sessionDao.insertSession(any()) } returns 5L
        coEvery { sessionDao.getSessionById(5) } returns SessionEntity(
            id = 5,
            userProfileId = 1,
            startedAt = now - 30_000L,
            endedAt = now,
            vocabulary = ""
        )
        every { correctionDao.getCorrectionsForSession(5) } returns flowOf(emptyList())

        val handle = repo.startSession(singleConfig)
        val result = repo.finalizeSession(handle, SessionStatus.COMPLETED)

        assertEquals(SessionResult.Outcome.DISCARDED_TOO_SHORT, result.outcome)
        assertTrue(result.summaries.isEmpty())
        coVerify { sessionDao.deleteSession(5) }
    }

    @Test
    fun `finalizeSession at least 60s sets status completed and returns SAVED`() = runTest {
        val now = System.currentTimeMillis()
        coEvery { sessionDao.insertSession(any()) } returns 7L
        val sessionSlot = slot<SessionEntity>()
        coEvery { sessionDao.updateSession(capture(sessionSlot)) } returns Unit
        coEvery { sessionDao.getSessionById(7) } returns SessionEntity(
            id = 7,
            userProfileId = 1,
            startedAt = now - 90_000L,
            endedAt = now,
            vocabulary = "hello\nworld"
        )
        every { correctionDao.getCorrectionsForSession(7) } returns flowOf(emptyList())
        coEvery { profileDao.getProfileById(1) } returns profileEntity

        val handle = repo.startSession(singleConfig)
        val result = repo.finalizeSession(handle, SessionStatus.COMPLETED)

        assertEquals(SessionResult.Outcome.SAVED, result.outcome)
        assertEquals(1, result.summaries.size)
        assertEquals("completed", sessionSlot.captured.status)
    }

    @Test
    fun `finalizeSession interrupted sets status partial`() = runTest {
        val now = System.currentTimeMillis()
        coEvery { sessionDao.insertSession(any()) } returns 8L
        val sessionSlot = slot<SessionEntity>()
        coEvery { sessionDao.updateSession(capture(sessionSlot)) } returns Unit
        coEvery { sessionDao.getSessionById(8) } returns SessionEntity(
            id = 8,
            userProfileId = 1,
            startedAt = now - 120_000L,
            endedAt = now,
            vocabulary = ""
        )
        every { correctionDao.getCorrectionsForSession(8) } returns flowOf(emptyList())
        coEvery { profileDao.getProfileById(1) } returns profileEntity

        val handle = repo.startSession(singleConfig)
        val result = repo.finalizeSession(handle, SessionStatus.PARTIAL)

        assertEquals(SessionResult.Outcome.SAVED, result.outcome)
        assertEquals("partial", sessionSlot.captured.status)
    }

    @Test
    fun `recoverDanglingSessions finalizes stray active session as partial`() = runTest {
        val now = System.currentTimeMillis()
        val activeSession = SessionEntity(
            id = 99,
            userProfileId = 1,
            startedAt = now - 120_000L,
            endedAt = now - 1_000L,
            status = "active",
            vocabulary = ""
        )
        coEvery { sessionDao.getActiveSessions() } returns listOf(activeSession)
        coEvery { sessionDao.getSessionById(99) } returns activeSession
        val turnEntities = listOf(
            ConversationTurnEntity(
                sessionId = 99,
                speakerProfileId = 1,
                turnIndex = 0,
                userText = "hi",
                aiText = "hello",
                timestamp = now - 60_000L
            )
        )
        coEvery { turnDao.getTurnsForSession(99) } returns turnEntities
        every { correctionDao.getCorrectionsForSession(99) } returns flowOf(emptyList())
        coEvery { profileDao.getProfileById(1) } returns profileEntity

        repo.recoverDanglingSessions()

        coVerify { sessionDao.updateSession(any()) }
    }
}
