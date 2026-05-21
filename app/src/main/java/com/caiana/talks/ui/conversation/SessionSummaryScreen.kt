package com.caiana.talks.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsProgressBar
import com.caiana.talks.ui.theme.components.LcarsTopBar

@Composable
fun SessionSummaryScreen(
    viewModel: SessionSummaryViewModel,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LcarsFrame(accentColor = LcarsColors.Orange) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(title = "Resumo da sessão", accentColor = LcarsColors.Orange)
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LcarsProgressBar(color = LcarsColors.Orange)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.perParticipant) { participant ->
                            ParticipantSummaryCard(participant = participant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LcarsButton(
                        onClick = onNavigateHome,
                        color = LcarsColors.Orange,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Voltar ao início")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantSummaryCard(participant: ParticipantSummaryUi) {
    LcarsDataPanel(accentColor = LcarsColors.Beige) {
        Text(
            text = participant.profileName,
            style = MaterialTheme.typography.headlineLarge,
            color = LcarsColors.Text,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Duração: ${participant.durationLabel}",
            style = MaterialTheme.typography.titleMedium,
            color = LcarsColors.TextDim,
        )
        Text(
            text = "Correções: ${participant.correctionCount}",
            style = MaterialTheme.typography.titleMedium,
            color = LcarsColors.TextDim,
        )
        if (participant.vocabularyHighlights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vocabulário desta sessão:",
                style = MaterialTheme.typography.labelMedium,
                color = LcarsColors.TextDim,
            )
            participant.vocabularyHighlights.forEach { word ->
                LcarsDataPanel(accentColor = LcarsColors.Blue, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = word, style = MaterialTheme.typography.bodyMedium, color = LcarsColors.Text)
                }
            }
        }
    }
}
