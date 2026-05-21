package com.caiana.talks.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoPracticeSetupScreen(
    viewModel: CoPracticeSetupViewModel = hiltViewModel(),
    onStart: (firstProfileId: Int, secondProfileId: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Co-practice") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Primeiro participante", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.profiles) { profile ->
                    ProfileSelectRow(
                        name = profile.name,
                        selected = state.firstSelectedId == profile.id,
                        onClick = { viewModel.onSelectFirst(profile.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Segundo participante", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.profiles) { profile ->
                    ProfileSelectRow(
                        name = profile.name,
                        selected = state.secondSelectedId == profile.id,
                        onClick = { viewModel.onSelectSecond(profile.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val first = state.firstSelectedId ?: return@Button
                    val second = state.secondSelectedId ?: return@Button
                    onStart(first, second)
                },
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar")
            }
        }
    }
}

@Composable
private fun ProfileSelectRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text = name, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
