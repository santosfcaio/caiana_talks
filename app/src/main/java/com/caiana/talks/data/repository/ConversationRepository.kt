package com.caiana.talks.data.repository

import com.caiana.talks.data.conversation.SessionDurationPolicy
import com.caiana.talks.data.local.db.ConversationTurnDao
import com.caiana.talks.data.local.db.ConversationTurnEntity
import com.caiana.talks.data.local.db.CorrectionDao
import com.caiana.talks.data.local.db.CorrectionEntity
import com.caiana.talks.data.local.db.SessionDao
import com.caiana.talks.data.local.db.SessionEntity
import com.caiana.talks.data.local.db.UserProfileDao
import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.ConversationSessionSummary
import com.caiana.talks.domain.model.SessionMode
import com.caiana.talks.domain.model.SessionResult
import com.caiana.talks.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

data class SessionHandle(
    val sessionId: Int,
    val groupId: String?,
    val startedAt: Long = System.currentTimeMillis(),
    val turnCount: Int = 0
) {
    fun withTurnCount(count: Int) = copy(turnCount = count)
}

interface ConversationRepository {
    suspend fun startSession(config: ConversationConfig): SessionHandle
    suspend fun appendTurn(
        handle: SessionHandle,
        speakerProfileId: Int,
        userText: String,
        aiText: String,
        meta: AiResponseMeta
    )
    suspend fun finalizeSession(handle: SessionHandle, status: SessionStatus): SessionResult
    suspend fun recoverDanglingSessions()
    suspend fun getSessionSummaryById(sessionId: Int): ConversationSessionSummary?
    suspend fun getSessionSummariesByGroup(groupId: String): List<ConversationSessionSummary>
}

class ConversationRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val turnDao: ConversationTurnDao,
    private val correctionDao: CorrectionDao,
    private val profileDao: UserProfileDao
) : ConversationRepository {

    override suspend fun startSession(config: ConversationConfig): SessionHandle {
        val now = System.currentTimeMillis()
        return if (config.mode == SessionMode.SINGLE) {
            val participant = config.participants.first()
            val entity = SessionEntity(
                userProfileId = participant.profileId,
                startedAt = now,
                endedAt = now,
                status = "active",
                mode = "single"
            )
            val id = sessionDao.insertSession(entity).toInt()
            SessionHandle(sessionId = id, groupId = null, startedAt = now)
        } else {
            val groupId = UUID.randomUUID().toString()
            var firstId = 0
            config.participants.forEachIndexed { index, participant ->
                val entity = SessionEntity(
                    userProfileId = participant.profileId,
                    startedAt = now,
                    endedAt = now,
                    status = "active",
                    mode = "dual",
                    coPracticeGroupId = groupId
                )
                val id = sessionDao.insertSession(entity).toInt()
                if (index == 0) firstId = id
            }
            SessionHandle(sessionId = firstId, groupId = groupId, startedAt = now)
        }
    }

    override suspend fun appendTurn(
        handle: SessionHandle,
        speakerProfileId: Int,
        userText: String,
        aiText: String,
        meta: AiResponseMeta
    ) {
        // Find the session row for this speaker
        val sessionId = if (handle.groupId != null) {
            sessionDao.getSessionsByGroup(handle.groupId)
                .firstOrNull { it.userProfileId == speakerProfileId }?.id ?: handle.sessionId
        } else {
            handle.sessionId
        }

        val session = sessionDao.getSessionById(sessionId) ?: return
        val turnIndex = turnDao.getTurnsForSession(sessionId).size

        turnDao.insertTurn(
            ConversationTurnEntity(
                sessionId = sessionId,
                speakerProfileId = speakerProfileId,
                turnIndex = turnIndex,
                userText = userText,
                aiText = aiText,
                timestamp = System.currentTimeMillis()
            )
        )

        meta.corrections.forEach { correction ->
            correctionDao.insertCorrection(
                CorrectionEntity(
                    sessionId = sessionId,
                    category = correction.category.name,
                    description = correction.note,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Append vocabulary to session row
        if (meta.vocabulary.isNotEmpty()) {
            val updated = session.copy(
                vocabulary = (session.vocabulary.lines() + meta.vocabulary)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            )
            sessionDao.updateSession(updated)
        }
    }

    override suspend fun finalizeSession(handle: SessionHandle, status: SessionStatus): SessionResult {
        val now = System.currentTimeMillis()
        val sessionIds = if (handle.groupId != null) {
            sessionDao.getSessionsByGroup(handle.groupId).map { it.id }
        } else {
            listOf(handle.sessionId)
        }

        val primarySession = sessionDao.getSessionById(sessionIds.first()) ?: return SessionResult(
            SessionResult.Outcome.DISCARDED_TOO_SHORT, emptyList()
        )
        val duration = now - primarySession.startedAt
        if (!SessionDurationPolicy.shouldKeep(duration)) {
            sessionIds.forEach { sessionDao.deleteSession(it) }
            return SessionResult(SessionResult.Outcome.DISCARDED_TOO_SHORT, emptyList())
        }

        val summaries = mutableListOf<ConversationSessionSummary>()
        sessionIds.forEach { sessionId ->
            val session = sessionDao.getSessionById(sessionId) ?: return@forEach
            val updated = session.copy(
                endedAt = now,
                status = status.name.lowercase()
            )
            sessionDao.updateSession(updated)

            val corrections = correctionDao.getCorrectionsForSession(sessionId).first()
            val profile = profileDao.getProfileById(session.userProfileId)
            summaries.add(
                ConversationSessionSummary(
                    sessionId = sessionId,
                    profileId = session.userProfileId,
                    profileName = profile?.name ?: "",
                    startedAt = session.startedAt,
                    endedAt = now,
                    correctionCount = corrections.size,
                    vocabulary = session.vocabulary.lines().filter { it.isNotBlank() },
                    coPracticeGroupId = handle.groupId
                )
            )
        }
        return SessionResult(SessionResult.Outcome.SAVED, summaries)
    }

    override suspend fun getSessionSummaryById(sessionId: Int): ConversationSessionSummary? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val corrections = correctionDao.getCorrectionsForSession(sessionId).first()
        val profile = profileDao.getProfileById(session.userProfileId)
        return ConversationSessionSummary(
            sessionId = sessionId,
            profileId = session.userProfileId,
            profileName = profile?.name ?: "",
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            correctionCount = corrections.size,
            vocabulary = session.vocabulary.lines().filter { it.isNotBlank() },
            coPracticeGroupId = session.coPracticeGroupId
        )
    }

    override suspend fun getSessionSummariesByGroup(groupId: String): List<ConversationSessionSummary> {
        return sessionDao.getSessionsByGroup(groupId).mapNotNull { session ->
            val corrections = correctionDao.getCorrectionsForSession(session.id).first()
            val profile = profileDao.getProfileById(session.userProfileId)
            ConversationSessionSummary(
                sessionId = session.id,
                profileId = session.userProfileId,
                profileName = profile?.name ?: "",
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                correctionCount = corrections.size,
                vocabulary = session.vocabulary.lines().filter { it.isNotBlank() },
                coPracticeGroupId = session.coPracticeGroupId
            )
        }
    }

    override suspend fun recoverDanglingSessions() {
        val activeSessions = sessionDao.getActiveSessions()
        val now = System.currentTimeMillis()
        activeSessions.forEach { session ->
            val lastTurn = turnDao.getTurnsForSession(session.id).lastOrNull()
            val endedAt = lastTurn?.timestamp ?: now
            val duration = endedAt - session.startedAt
            if (SessionDurationPolicy.shouldKeep(duration)) {
                sessionDao.updateSession(session.copy(endedAt = endedAt, status = "partial"))
            } else {
                sessionDao.deleteSession(session.id)
            }
        }
    }
}
