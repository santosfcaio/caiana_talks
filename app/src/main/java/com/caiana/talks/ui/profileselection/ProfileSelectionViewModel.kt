package com.caiana.talks.ui.profileselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.local.db.UserProfileEntity
import com.caiana.talks.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileSelectionUiState {
    object Loading : ProfileSelectionUiState()
    data class ShowSelection(val profiles: List<UserProfileEntity>) : ProfileSelectionUiState()
}

@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileSelectionUiState> = repository.getAllProfiles()
        .map { profiles -> ProfileSelectionUiState.ShowSelection(profiles) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileSelectionUiState.Loading
        )

    fun onUserSelected(id: Int) {
        viewModelScope.launch { repository.selectUser(id) }
    }

    fun clearAndReselect() {
        viewModelScope.launch { repository.clearActiveUser() }
    }
}
