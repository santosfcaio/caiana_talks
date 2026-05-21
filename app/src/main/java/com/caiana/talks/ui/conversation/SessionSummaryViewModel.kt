package com.caiana.talks.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSummaryUiState(
    val isLoading: Boolean = true,
    val perParticipant: List<ParticipantSummaryUi> = emptyList()
)

data class ParticipantSummaryUi(
    val profileName: String,
    val durationLabel: String,
    val correctionCount: Int,
    val vocabularyHighlights: List<String>
)

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ConversationRepository
) : ViewModel() {

    private val sessionId: Int? = savedStateHandle.get<String?>("sessionId")?.toIntOrNull()
    private val groupId: String? = savedStateHandle.get<String?>("groupId")

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummaries()
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            val summaries = if (groupId != null) {
                repository.getSessionSummariesByGroup(groupId)
            } else if (sessionId != null) {
                listOfNotNull(repository.getSessionSummaryById(sessionId))
            } else emptyList()

            val participants = summaries.map { s ->
                ParticipantSummaryUi(
                    profileName = s.profileName,
                    durationLabel = formatDuration(s.endedAt - s.startedAt),
                    correctionCount = s.correctionCount,
                    vocabularyHighlights = s.vocabulary
                )
            }
            _uiState.update { it.copy(isLoading = false, perParticipant = participants) }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalMinutes = (durationMs / 60_000L).toInt()
        return if (totalMinutes < 60) {
            "${totalMinutes}min"
        } else {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            "${hours}h ${minutes}min"
        }
    }
}
