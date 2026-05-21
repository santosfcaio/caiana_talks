package com.caiana.talks.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsTopBar

@Composable
fun HomeScreen(
    userName: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToConversation: () -> Unit = {},
    onNavigateToCoPractice: () -> Unit = {}
) {
    LcarsFrame(accentColor = LcarsColors.Orange) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = "Caiana Talks",
                accentColor = LcarsColors.Orange,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações",
                            tint = LcarsColors.Black)
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Olá, $userName!",
                    style = MaterialTheme.typography.displayMedium,
                    color = LcarsColors.Text,
                )
                Spacer(Modifier.height(32.dp))
                LcarsButton(
                    onClick = onNavigateToConversation,
                    color = LcarsColors.Orange,
                ) {
                    Text("Iniciar conversa")
                }
                Spacer(Modifier.height(12.dp))
                LcarsButton(
                    onClick = onNavigateToCoPractice,
                    color = LcarsColors.Blue,
                ) {
                    Text("Conversa em dupla")
                }
                Spacer(Modifier.height(12.dp))
                LcarsButton(
                    onClick = onNavigateToStats,
                    color = LcarsColors.Beige,
                    textColor = LcarsColors.Black,
                ) {
                    Text("Ver meu progresso")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(userName = "Caio", onNavigateToSettings = {}, onNavigateToStats = {})
}
