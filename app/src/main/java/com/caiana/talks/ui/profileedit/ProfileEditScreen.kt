package com.caiana.talks.ui.profileedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsCheckRow
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsOptionPills
import com.caiana.talks.ui.theme.components.LcarsTopBar

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

    LcarsFrame(accentColor = LcarsColors.Blue) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = "Editar perfil",
                accentColor = LcarsColors.Blue,
                navigationIcon = if (!hideBack) {
                    {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = LcarsColors.Black,
                            )
                        }
                    }
                } else null,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                LcarsDataPanel(accentColor = LcarsColors.Orange) {
                    Text(
                        "Meta de aprendizado",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Orange,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LcarsOptionPills(
                    options = LearningGoal.entries.map { it to it.displayLabel },
                    selected = uiState.learningGoal,
                    onSelect = viewModel::setLearningGoal,
                    accentColor = LcarsColors.Blue,
                )

                Spacer(modifier = Modifier.height(24.dp))

                LcarsDataPanel(accentColor = LcarsColors.Orange) {
                    Text(
                        "Temas de conversa preferidos",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Orange,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ConversationTheme.entries.forEach { theme ->
                    LcarsCheckRow(
                        label = theme.displayLabel,
                        checked = theme in uiState.selectedThemes,
                        onCheckedChange = { viewModel.toggleTheme(theme) },
                        accentColor = LcarsColors.Blue,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                LcarsDataPanel(accentColor = LcarsColors.Orange) {
                    Text(
                        "Configurações de voz",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Orange,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Gênero", style = MaterialTheme.typography.labelMedium, color = LcarsColors.TextDim)
                LcarsOptionPills(
                    options = VoiceGender.entries.map { it to it.displayLabel },
                    selected = uiState.voiceGender,
                    onSelect = viewModel::setVoiceGender,
                    accentColor = LcarsColors.Blue,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Sotaque", style = MaterialTheme.typography.labelMedium, color = LcarsColors.TextDim)
                LcarsOptionPills(
                    options = VoiceAccent.entries.map { it to it.displayLabel },
                    selected = uiState.voiceAccent,
                    onSelect = viewModel::setVoiceAccent,
                    accentColor = LcarsColors.Blue,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Velocidade da fala", style = MaterialTheme.typography.labelMedium, color = LcarsColors.TextDim)
                LcarsOptionPills(
                    options = SpeechRate.entries.map { it to it.displayLabel },
                    selected = uiState.speechRate,
                    onSelect = viewModel::setSpeechRate,
                    accentColor = LcarsColors.Purple,
                )

                Spacer(modifier = Modifier.height(32.dp))

                LcarsButton(
                    onClick = { viewModel.savePreferences() },
                    color = LcarsColors.Orange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Salvar")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
