package com.caiana.talks.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StartDestination {
    object Loading : StartDestination()
    object ProfileSelection : StartDestination()
    data class Home(val userName: String) : StartDestination()
    data class ProfileSetup(val userName: String) : StartDestination()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val startDestination: StateFlow<StartDestination> = repository.getActiveUserProfile()
        .map { profile ->
            when {
                profile == null -> StartDestination.ProfileSelection
                profile.learningGoals.isBlank() -> StartDestination.ProfileSetup(profile.name)
                else -> StartDestination.Home(profile.name)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StartDestination.Loading
        )

    fun clearActiveUser() {
        viewModelScope.launch { repository.clearActiveUser() }
    }
}
