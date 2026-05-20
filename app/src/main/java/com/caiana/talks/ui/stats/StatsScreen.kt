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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.domain.model.SessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Progresso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
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

@Composable
private fun CefrLevelCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nível de Inglês", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (uiState.cefrLevel == null) {
                Text("Conclua uma sessão para ver seu nível estimado.")
            } else {
                Text(uiState.cefrLevel.label, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.cefrLevel.description)
            }
        }
    }
}

@Composable
private fun ErrorBreakdownCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Erros por Categoria", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ErrorRow(label = "Gramática", count = uiState.grammarErrors)
            ErrorRow(label = "Vocabulário", count = uiState.vocabularyErrors)
            ErrorRow(label = "Fluência", count = uiState.fluencyErrors)
        }
    }
}

@Composable
private fun ErrorRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("$count erro${if (count == 1) "" else "s"}")
    }
}

@Composable
private fun SessionHistoryCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Histórico de Sessões", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (uiState.sessions.isEmpty()) {
                Text("Nenhuma sessão concluída ainda.")
            } else {
                uiState.sessions.forEachIndexed { index, session ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SessionItem(session)
                }
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
        style = MaterialTheme.typography.labelLarge
    )
    Spacer(modifier = Modifier.height(4.dp))
    if (session.corrections.isEmpty()) {
        Text("• Nenhuma correção registrada.")
    } else {
        session.corrections.forEach { correction ->
            Text("• ${correction.category.displayLabel}: \"${correction.description}\"")
        }
    }
}

@Composable
private fun InsightsCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Insights", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (uiState.insights.isEmpty()) {
                Text("Conclua mais sessões para desbloquear insights.")
            } else {
                uiState.insights.forEach { insight ->
                    Text("• $insight")
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun formatDuration(durationMinutes: Int): String {
    return if (durationMinutes < 60) {
        "${durationMinutes}min"
    } else {
        "${durationMinutes / 60}h ${durationMinutes % 60}min"
    }
}
