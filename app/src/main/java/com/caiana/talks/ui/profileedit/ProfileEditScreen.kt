package com.caiana.talks.ui.profileedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: ProfileEditViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    hideBack: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    if (hideBack) {
        BackHandler(enabled = true) { /* block back navigation during onboarding */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
                navigationIcon = {
                    if (!hideBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Meta de aprendizado ---
            Text("Meta de aprendizado", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            LearningGoal.entries.forEach { goal ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = uiState.learningGoal == goal,
                        onClick = { viewModel.setLearningGoal(goal) }
                    )
                    Text(goal.displayLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Temas de conversa preferidos ---
            Text("Temas de conversa preferidos", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            ConversationTheme.entries.forEach { theme ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = theme in uiState.selectedThemes,
                        onCheckedChange = { viewModel.toggleTheme(theme) }
                    )
                    Text(theme.displayLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Configurações de voz ---
            Text("Configurações de voz", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gênero", style = MaterialTheme.typography.labelMedium)
            VoiceGender.entries.forEach { gender ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = uiState.voiceGender == gender,
                        onClick = { viewModel.setVoiceGender(gender) }
                    )
                    Text(gender.displayLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Sotaque", style = MaterialTheme.typography.labelMedium)
            VoiceAccent.entries.forEach { accent ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = uiState.voiceAccent == accent,
                        onClick = { viewModel.setVoiceAccent(accent) }
                    )
                    Text(accent.displayLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Velocidade da fala", style = MaterialTheme.typography.labelMedium)
            SpeechRate.entries.forEach { rate ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = uiState.speechRate == rate,
                        onClick = { viewModel.setSpeechRate(rate) }
                    )
                    Text(rate.displayLabel, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.savePreferences() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
