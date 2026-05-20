package com.caiana.talks.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.repository.StatsRepository
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.CefrLevel
import com.caiana.talks.domain.model.SessionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val cefrLevel: CefrLevel? = null,
    val grammarErrors: Int = 0,
    val vocabularyErrors: Int = 0,
    val fluencyErrors: Int = 0,
    val sessions: List<SessionSummary> = emptyList(),
    val insights: List<String> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = userRepository.getActiveUserProfile()
        .flatMapLatest { profile ->
            if (profile == null) {
                flowOf(StatsUiState(isLoading = false))
            } else {
                statsRepository.getProgressSnapshot(profile.id).map { snapshot ->
                    StatsUiState(
                        isLoading = false,
                        cefrLevel = snapshot.cefrLevel,
                        grammarErrors = snapshot.grammarErrors,
                        vocabularyErrors = snapshot.vocabularyErrors,
                        fluencyErrors = snapshot.fluencyErrors,
                        sessions = snapshot.sessions,
                        insights = snapshot.insights
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(isLoading = true)
        )
}
