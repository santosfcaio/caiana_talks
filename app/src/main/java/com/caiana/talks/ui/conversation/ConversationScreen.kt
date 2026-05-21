package com.caiana.talks.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
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

    // Handle navigation when session ends
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.aiPersonaName) },
                actions = {
                    IconButton(onClick = { viewModel.onEndSession() }) {
                        Icon(Icons.Default.Close, contentDescription = "Encerrar sessão")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Turn history
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.turns) { turn ->
                    TurnCard(turn = turn)
                }
            }

            // Portuguese nudge
            if (state.portugueseNudgeVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Continue em inglês! You're doing great.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Audio spectrum — visible while AI speaks
            AudioSpectrum(
                speaking = state.phase == ConversationUiState.Phase.SPEAKING,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Phase indicator and live transcript
            val phaseLabel = when (state.phase) {
                ConversationUiState.Phase.IDLE -> "Aguardando..."
                ConversationUiState.Phase.LISTENING -> state.activeSpeakerName?.let { "Ouvindo $it..." } ?: "Ouvindo..."
                ConversationUiState.Phase.THINKING -> "Processando..."
                ConversationUiState.Phase.SPEAKING -> "${state.aiPersonaName} está respondendo..."
                ConversationUiState.Phase.ENDED -> "Sessão encerrada"
            }
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.titleMedium
            )
            if (state.liveTranscript.isNotBlank()) {
                Text(
                    text = state.liveTranscript,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Error state
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
                Text(text = msg, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.onRetry() }) {
                    Text("Tentar novamente")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.onEndSession() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Encerrar sessão")
            }
        }
    }
}

@Composable
private fun TurnCard(turn: TurnUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = turn.speakerName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = turn.userText, style = MaterialTheme.typography.bodyMedium)
            if (turn.aiText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = turn.aiText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (turn.vocabulary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vocabulário: ${turn.vocabulary.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
