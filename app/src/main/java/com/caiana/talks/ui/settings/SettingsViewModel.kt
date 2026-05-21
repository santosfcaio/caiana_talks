package com.caiana.talks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    val apiKeyOverride: StateFlow<String> = userPrefs.openrouterApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val modelOverride: StateFlow<String> = userPrefs.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setApiKey(key: String) {
        viewModelScope.launch { userPrefs.setOpenrouterApiKey(key) }
    }

    fun setModel(model: String) {
        viewModelScope.launch { userPrefs.setAiModel(model) }
    }
}
