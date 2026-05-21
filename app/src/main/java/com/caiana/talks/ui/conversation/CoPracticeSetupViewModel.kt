package com.caiana.talks.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoPracticeSetupUiState(
    val profiles: List<UserProfileEntity> = emptyList(),
    val firstSelectedId: Int? = null,
    val secondSelectedId: Int? = null,
    val canStart: Boolean = false
)

@HiltViewModel
class CoPracticeSetupViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoPracticeSetupUiState())
    val uiState: StateFlow<CoPracticeSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllProfiles().collect { profiles ->
                _uiState.update { it.copy(profiles = profiles) }
            }
        }
    }

    fun onSelectFirst(profileId: Int) {
        _uiState.update { state ->
            val newState = state.copy(firstSelectedId = profileId)
            newState.copy(canStart = canStart(profileId, newState.secondSelectedId))
        }
    }

    fun onSelectSecond(profileId: Int) {
        _uiState.update { state ->
            val newState = state.copy(secondSelectedId = profileId)
            newState.copy(canStart = canStart(newState.firstSelectedId, profileId))
        }
    }

    private fun canStart(firstId: Int?, secondId: Int?): Boolean =
        firstId != null && secondId != null && firstId != secondId
}
