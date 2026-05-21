package com.caiana.talks.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.domain.model.SessionSummary
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsProgressBar
import com.caiana.talks.ui.theme.components.LcarsTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LcarsFrame(accentColor = LcarsColors.Blue) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = "Meu Progresso",
                accentColor = LcarsColors.Blue,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = LcarsColors.Black,
                        )
                    }
                }
            )
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LcarsProgressBar(color = LcarsColors.Blue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { CefrLevelCard(uiState) }
                    item { ErrorBreakdownCard(uiState) }
                    item { SessionHistoryCard(uiState) }
                    item { InsightsCard(uiState) }
                }
            }
        }
    }
}

@Composable
private fun CefrLevelCard(uiState: StatsUiState) {
    LcarsDataPanel(accentColor = LcarsColors.Blue) {
        Text("Nível de Inglês", style = MaterialTheme.typography.titleMedium, color = LcarsColors.Text)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LcarsColors.Blue)
        if (uiState.cefrLevel == null) {
            Text("Conclua uma sessão para ver seu nível estimado.", color = LcarsColors.TextDim)
        } else {
            Text(
                uiState.cefrLevel.label,
                style = MaterialTheme.typography.displayLarge,
                color = LcarsColors.Orange,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(uiState.cefrLevel.description, color = LcarsColors.Text)
        }
    }
}

@Composable
private fun ErrorBreakdownCard(uiState: StatsUiState) {
    LcarsDataPanel(accentColor = LcarsColors.Blue) {
        Text("Erros por Categoria", style = MaterialTheme.typography.titleMedium, color = LcarsColors.Text)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LcarsColors.Blue)
        ErrorRow(label = "Gramática", count = uiState.grammarErrors)
        ErrorRow(label = "Vocabulário", count = uiState.vocabularyErrors)
        ErrorRow(label = "Fluência", count = uiState.fluencyErrors)
    }
}

@Composable
private fun ErrorRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LcarsColors.Text)
        Text(
            "$count erro${if (count == 1) "" else "s"}",
            style = MaterialTheme.typography.titleMedium,
            color = LcarsColors.Text,
        )
    }
}

@Composable
private fun SessionHistoryCard(uiState: StatsUiState) {
    LcarsDataPanel(accentColor = LcarsColors.Blue) {
        Text("Histórico de Sessões", style = MaterialTheme.typography.titleMedium, color = LcarsColors.Text)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LcarsColors.Blue)
        if (uiState.sessions.isEmpty()) {
            Text("Nenhuma sessão concluída ainda.", color = LcarsColors.TextDim)
        } else {
            uiState.sessions.forEachIndexed { index, session ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LcarsColors.Blue)
                SessionItem(session)
            }
        }
    }
}

@Composable
private fun SessionItem(session: SessionSummary) {
    val dateText = SimpleDateFormat("dd MMM yyyy", Locale("pt", "BR"))
        .format(Date(session.date))
    val durationText = formatDuration(session.durationMinutes)

    Text(
        "$dateText · $durationText",
        style = MaterialTheme.typography.titleMedium,
        color = LcarsColors.Text,
    )
    Spacer(modifier = Modifier.height(4.dp))
    if (session.corrections.isEmpty()) {
        Text("• Nenhuma correção registrada.", style = MaterialTheme.typography.bodyMedium, color = LcarsColors.TextDim)
    } else {
        session.corrections.forEach { correction ->
            Text(
                "• ${correction.category.displayLabel}: \"${correction.description}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = LcarsColors.TextDim,
            )
        }
    }
}

@Composable
private fun InsightsCard(uiState: StatsUiState) {
    LcarsDataPanel(accentColor = LcarsColors.Beige) {
        Text("Insights", style = MaterialTheme.typography.titleMedium, color = LcarsColors.Text)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LcarsColors.Beige)
        if (uiState.insights.isEmpty()) {
            Text("Conclua mais sessões para desbloquear insights.", color = LcarsColors.TextDim)
        } else {
            uiState.insights.forEach { insight ->
                Text("• $insight", style = MaterialTheme.typography.bodyMedium, color = LcarsColors.Text)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun formatDuration(durationMinutes: Int): String {
    return if (durationMinutes < 60) "${durationMinutes}min"
           else "${durationMinutes / 60}h ${durationMinutes % 60}min"
}
