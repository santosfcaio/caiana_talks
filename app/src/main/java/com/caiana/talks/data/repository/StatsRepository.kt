package com.caiana.talks.data.repository

import com.caiana.talks.data.local.db.CategoryCount
import com.caiana.talks.data.local.db.CorrectionDao
import com.caiana.talks.data.local.db.CorrectionEntity
import com.caiana.talks.data.local.db.SessionDao
import com.caiana.talks.data.local.db.SessionEntity
import com.caiana.talks.domain.model.CefrLevel
import com.caiana.talks.domain.model.CorrectionCategory
import com.caiana.talks.domain.model.CorrectionSummary
import com.caiana.talks.domain.model.ProgressSnapshot
import com.caiana.talks.domain.model.SessionSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface StatsRepository {
    fun getProgressSnapshot(profileId: Int): Flow<ProgressSnapshot>
}

class StatsRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val correctionDao: CorrectionDao
) : StatsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getProgressSnapshot(profileId: Int): Flow<ProgressSnapshot> {
        return sessionDao.getSessionsForProfile(profileId)
            .flatMapLatest { sessions ->
                if (sessions.isEmpty()) {
                    correctionDao.getCategoryCountsForProfile(profileId).map { _ -> emptySnapshot() }
                } else {
                    buildSnapshotFlow(profileId, sessions)
                }
            }
    }

    private fun buildSnapshotFlow(
        profileId: Int,
        sessions: List<SessionEntity>
    ): Flow<ProgressSnapshot> {
        val categoryCountsFlow = correctionDao.getCategoryCountsForProfile(profileId)
        val correctionFlows = sessions.map { session ->
            correctionDao.getCorrectionsForSession(session.id).map { corrections ->
                session to corrections
            }
        }
        val allCorrectionsFlow: Flow<List<Pair<SessionEntity, List<CorrectionEntity>>>> =
            combine(correctionFlows) { it.toList() }

        return combine(allCorrectionsFlow, categoryCountsFlow) { sessionDataList, categoryCounts ->
            buildSnapshot(sessions.size, sessionDataList, categoryCounts)
        }
    }

    private fun buildSnapshot(
        sessionCount: Int,
        sessionDataList: List<Pair<SessionEntity, List<CorrectionEntity>>>,
        categoryCounts: List<CategoryCount>
    ): ProgressSnapshot {
        val grammar = categoryCounts.firstOrNull { it.category == "GRAMMAR" }?.count ?: 0
        val vocabulary = categoryCounts.firstOrNull { it.category == "VOCABULARY" }?.count ?: 0
        val fluency = categoryCounts.firstOrNull { it.category == "FLUENCY" }?.count ?: 0
        val totalErrors = grammar + vocabulary + fluency

        return ProgressSnapshot(
            cefrLevel = computeCefrLevel(sessionCount, totalErrors),
            grammarErrors = grammar,
            vocabularyErrors = vocabulary,
            fluencyErrors = fluency,
            sessions = sessionDataList.map { (session, corrections) ->
                toSessionSummary(session, corrections)
            },
            insights = computeInsights(
                sessionCount, grammar, vocabulary, fluency, totalErrors, sessionDataList
            )
        )
    }

    private fun emptySnapshot() = ProgressSnapshot(
        cefrLevel = null,
        grammarErrors = 0,
        vocabularyErrors = 0,
        fluencyErrors = 0,
        sessions = emptyList(),
        insights = listOf("Complete mais sessões para desbloquear insights.")
    )

    internal fun computeCefrLevel(sessionCount: Int, totalErrors: Int): CefrLevel? {
        if (sessionCount == 0) return null
        val avg = totalErrors.toDouble() / sessionCount
        return when {
            sessionCount < 3 || avg > 15 -> CefrLevel.A1
            sessionCount in 3..5 && avg > 10 -> CefrLevel.A2
            sessionCount in 6..10 && avg > 5 -> CefrLevel.B1
            sessionCount in 11..20 && avg > 2 -> CefrLevel.B2
            sessionCount > 20 && avg >= 1.0 && avg <= 2.0 -> CefrLevel.C1
            sessionCount > 30 && avg < 1.0 -> CefrLevel.C2
            else -> CefrLevel.B1
        }
    }

    internal fun computeInsights(
        sessionCount: Int,
        grammar: Int,
        vocabulary: Int,
        fluency: Int,
        totalErrors: Int,
        sessionDataList: List<Pair<SessionEntity, List<CorrectionEntity>>>
    ): List<String> {
        if (sessionCount < 2) {
            return listOf("Complete mais sessões para desbloquear insights.")
        }

        val insights = mutableListOf<String>()

        if (totalErrors > 0) {
            val grammarPct = grammar.toDouble() / totalErrors
            val vocabPct = vocabulary.toDouble() / totalErrors
            val fluencyPct = fluency.toDouble() / totalErrors

            if (grammarPct > 0.5) {
                insights += "Você comete mais erros de gramática. Foque nos tempos verbais e estrutura das frases."
            }
            if (insights.size < 3 && vocabPct > 0.5) {
                insights += "Expandir seu vocabulário é sua maior área de crescimento. Leia e ouça em inglês diariamente."
            }
            if (insights.size < 3 && fluencyPct > 0.5) {
                insights += "Seu principal desafio é a fluência. Tente falar de forma mais contínua, sem pausas longas."
            }
        }

        if (insights.size < 3 && sessionCount >= 3) {
            val lastThree = sessionDataList.take(3)
            val topCategories = lastThree.map { (_, corrections) ->
                corrections.groupBy { it.category }.maxByOrNull { it.value.size }?.key
            }
            val consistent = topCategories.firstOrNull() != null &&
                topCategories.all { it == topCategories.first() }
            if (consistent) {
                val label = CorrectionCategory.entries
                    .firstOrNull { it.name == topCategories.first() }?.displayLabel
                    ?: topCategories.first()
                insights += "Você comete erros frequentes de $label nas últimas sessões — considere prática dedicada nessa área."
            }
        }

        if (insights.size < 3 && sessionCount >= 5) {
            val recentRate = sessionDataList.take(3).map { it.second.size }.average()
            val earliestRate = sessionDataList.takeLast(3).map { it.second.size }.average()
            if (recentRate < earliestRate) {
                insights += "Ótimo progresso! Sua taxa de erros está diminuindo. Continue assim!"
            }
        }

        if (insights.isEmpty()) {
            insights += "Continue praticando! Cada sessão te aproxima da fluência."
        }

        return insights.take(3)
    }

    private fun toSessionSummary(
        session: SessionEntity,
        corrections: List<CorrectionEntity>
    ): SessionSummary {
        val durationMinutes = ((session.endedAt - session.startedAt) / 60_000L)
            .toInt().coerceAtLeast(0)
        val correctionSummaries = corrections.take(5).mapNotNull { entity ->
            val category = CorrectionCategory.entries.firstOrNull { it.name == entity.category }
                ?: return@mapNotNull null
            CorrectionSummary(category = category, description = entity.description)
        }
        return SessionSummary(
            id = session.id,
            date = session.startedAt,
            durationMinutes = durationMinutes,
            corrections = correctionSummaries
        )
    }
}
