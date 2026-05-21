package com.caiana.talks.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsTextField
import com.caiana.talks.ui.theme.components.LcarsTopBar

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onSwitchProfile: () -> Unit,
    onNavigateToEdit: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val apiKey by viewModel.apiKeyOverride.collectAsStateWithLifecycle()
    val model by viewModel.modelOverride.collectAsStateWithLifecycle()

    LcarsFrame(accentColor = LcarsColors.Purple) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = "Configurações",
                accentColor = LcarsColors.Purple,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                LcarsDataPanel(accentColor = LcarsColors.Orange) {
                    Text(
                        text = "Conta",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Orange,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LcarsButton(
                    onClick = onNavigateToEdit,
                    color = LcarsColors.Orange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Editar preferências")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LcarsButton(
                    onClick = onSwitchProfile,
                    color = LcarsColors.Blue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Trocar perfil")
                }
                Spacer(modifier = Modifier.height(24.dp))
                LcarsDataPanel(accentColor = LcarsColors.Blue) {
                    Text(
                        text = "API / Modelo",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Blue,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LcarsTextField(
                    value = apiKey,
                    onValueChange = { viewModel.setApiKey(it) },
                    label = "Chave OpenRouter",
                    placeholder = "Usar padrão do app",
                )
                Spacer(modifier = Modifier.height(12.dp))
                LcarsTextField(
                    value = model,
                    onValueChange = { viewModel.setModel(it) },
                    label = "Modelo de IA",
                    placeholder = "Usar padrão do app",
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Configurações")
@Composable
private fun SettingsScreenPreview() {
    SettingsScreenContent(
        apiKey = "",
        model = "qwen/qwen3.5-9b",
        onNavigateBack = {},
        onSwitchProfile = {},
        onNavigateToEdit = {},
        onApiKeyChange = {},
        onModelChange = {},
    )
}

@Composable
private fun SettingsScreenContent(
    apiKey: String,
    model: String,
    onNavigateBack: () -> Unit,
    onSwitchProfile: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    LcarsFrame(accentColor = LcarsColors.Purple) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(
                title = "Configurações",
                accentColor = LcarsColors.Purple,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                LcarsDataPanel(accentColor = LcarsColors.Orange) {
                    Text(
                        text = "Conta",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Orange,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LcarsButton(
                    onClick = onNavigateToEdit,
                    color = LcarsColors.Orange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Editar preferências")
                }
                Spacer(modifier = Modifier.height(8.dp))
                LcarsButton(
                    onClick = onSwitchProfile,
                    color = LcarsColors.Blue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Trocar perfil")
                }
                Spacer(modifier = Modifier.height(24.dp))
                LcarsDataPanel(accentColor = LcarsColors.Blue) {
                    Text(
                        text = "API / Modelo",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Blue,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LcarsTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = "Chave OpenRouter",
                    placeholder = "Usar padrão do app",
                )
                Spacer(modifier = Modifier.height(12.dp))
                LcarsTextField(
                    value = model,
                    onValueChange = onModelChange,
                    label = "Modelo de IA",
                    placeholder = "Usar padrão do app",
                )
            }
        }
    }
}
