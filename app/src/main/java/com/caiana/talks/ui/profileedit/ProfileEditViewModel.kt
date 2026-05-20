package com.caiana.talks.ui.profileedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.local.db.toLearningGoal
import com.caiana.talks.data.local.db.toSelectedThemes
import com.caiana.talks.data.local.db.toSpeechRate
import com.caiana.talks.data.local.db.toVoiceAccent
import com.caiana.talks.data.local.db.toVoiceGender
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val profileId: Int = 0,
    val profileName: String = "",
    val learningGoal: LearningGoal? = null,
    val selectedThemes: Set<ConversationTheme> = emptySet(),
    val voiceGender: VoiceGender = VoiceGender.FEMININE,
    val voiceAccent: VoiceAccent = VoiceAccent.AMERICAN,
    val speechRate: SpeechRate = SpeechRate.NORMAL,
    val isSaved: Boolean = false
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveUserProfile().collect { entity ->
                if (entity != null && !_uiState.value.isSaved) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profileId = entity.id,
                            profileName = entity.name,
                            learningGoal = entity.toLearningGoal(),
                            selectedThemes = entity.toSelectedThemes(),
                            voiceGender = entity.toVoiceGender(),
                            voiceAccent = entity.toVoiceAccent(),
                            speechRate = entity.toSpeechRate()
                        )
                    }
                }
            }
        }
    }

    fun setLearningGoal(goal: LearningGoal) {
        _uiState.update { it.copy(learningGoal = goal) }
    }

    fun toggleTheme(theme: ConversationTheme) {
        _uiState.update { state ->
            val themes = state.selectedThemes
            state.copy(selectedThemes = if (theme in themes) themes - theme else themes + theme)
        }
    }

    fun setVoiceGender(gender: VoiceGender) {
        _uiState.update { it.copy(voiceGender = gender) }
    }

    fun setVoiceAccent(accent: VoiceAccent) {
        _uiState.update { it.copy(voiceAccent = accent) }
    }

    fun setSpeechRate(rate: SpeechRate) {
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun savePreferences() {
        val state = _uiState.value
        viewModelScope.launch {
            val entity = UserProfileEntity(
                id = state.profileId,
                name = state.profileName,
                learningGoals = state.learningGoal?.id ?: "",
                preferredThemes = state.selectedThemes.joinToString(",") { it.id },
                aiVoiceGender = state.voiceGender.id,
                aiVoiceAccent = state.voiceAccent.id,
                aiSpeechRate = state.speechRate.id
            )
            repository.updateProfile(entity)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
