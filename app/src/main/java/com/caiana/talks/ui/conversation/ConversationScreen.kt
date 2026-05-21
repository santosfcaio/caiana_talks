package com.caiana.talks.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsStatusIndicator
import com.caiana.talks.ui.theme.components.LcarsTopBar

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateToSummary: (groupId: String?, sessionId: Int) -> Unit,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onStart()
        else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(state.sessionResult) {
        val result = state.sessionResult ?: return@LaunchedEffect
        when (result.outcome) {
            com.caiana.talks.domain.model.SessionResult.Outcome.SAVED -> {
                val summary = result.summaries.firstOrNull()
                onNavigateToSummary(summary?.coPracticeGroupId, summary?.sessionId ?: 0)
            }
            com.caiana.talks.domain.model.SessionResult.Outcome.DISCARDED_TOO_SHORT -> onNavigateHome()
        }
    }

    LcarsFrame(accentColor = LcarsColors.Maroon) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = state.aiPersonaName,
                accentColor = LcarsColors.Maroon,
                actions = {
                    IconButton(onClick = { viewModel.onEndSession() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Encerrar sessão",
                            tint = LcarsColors.Orange,
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.turns) { turn ->
                        TurnCard(turn = turn)
                    }
                }

                if (state.portugueseNudgeVisible) {
                    LcarsDataPanel(accentColor = LcarsColors.Beige) {
                        Text(
                            text = "Continue em inglês! You're doing great.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LcarsColors.Text,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                AudioSpectrum(
                    speaking = state.phase == ConversationUiState.Phase.SPEAKING,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                val phaseLabel = when (state.phase) {
                    ConversationUiState.Phase.IDLE -> "Aguardando..."
                    ConversationUiState.Phase.LISTENING -> state.activeSpeakerName?.let { "Ouvindo $it..." } ?: "Ouvindo..."
                    ConversationUiState.Phase.THINKING -> "Processando..."
                    ConversationUiState.Phase.SPEAKING -> "${state.aiPersonaName} está respondendo..."
                    ConversationUiState.Phase.ENDED -> "Sessão encerrada"
                }
                val blinking = state.phase == ConversationUiState.Phase.LISTENING
                        || state.phase == ConversationUiState.Phase.THINKING
                LcarsStatusIndicator(
                    label = phaseLabel,
                    color = LcarsColors.Orange,
                    blinking = blinking,
                )
                if (state.liveTranscript.isNotBlank()) {
                    Text(
                        text = state.liveTranscript,
                        style = MaterialTheme.typography.bodySmall,
                        color = LcarsColors.TextDim,
                    )
                }

                state.error?.let { err ->
                    val msg = when (err) {
                        com.caiana.talks.domain.model.ConversationError.MIC_PERMISSION_DENIED ->
                            "Permissão de microfone negada. Habilite nas configurações."
                        com.caiana.talks.domain.model.ConversationError.MIC_UNAVAILABLE ->
                            "Microfone indisponível."
                        com.caiana.talks.domain.model.ConversationError.NETWORK_UNAVAILABLE ->
                            "Sem conexão com a internet."
                        com.caiana.talks.domain.model.ConversationError.AI_API_ERROR ->
                            "Erro ao contatar o tutor. Tente novamente."
                        com.caiana.talks.domain.model.ConversationError.STORAGE_FULL ->
                            "Armazenamento cheio. A sessão foi salva parcialmente."
                    }
                    LcarsDataPanel(accentColor = LcarsColors.Maroon) {
                        Text(text = msg, color = LcarsColors.Text, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LcarsButton(
                            onClick = { viewModel.onRetry() },
                            color = LcarsColors.Maroon,
                        ) {
                            Text("Tentar novamente")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LcarsButton(
                    onClick = { viewModel.onEndSession() },
                    color = LcarsColors.Maroon,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Encerrar sessão")
                }
            }
        }
    }
}

@Composable
private fun TurnCard(turn: TurnUi) {
    LcarsDataPanel(accentColor = LcarsColors.Orange) {
        Text(
            text = turn.speakerName,
            style = MaterialTheme.typography.titleMedium,
            color = LcarsColors.Orange,
        )
        Text(text = turn.userText, style = MaterialTheme.typography.bodyMedium, color = LcarsColors.Text)
        if (turn.aiText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = turn.aiText,
                style = MaterialTheme.typography.bodySmall,
                color = LcarsColors.TextDim,
            )
        }
        if (turn.vocabulary.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vocabulário: ${turn.vocabulary.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = LcarsColors.Blue,
            )
        }
    }
}
