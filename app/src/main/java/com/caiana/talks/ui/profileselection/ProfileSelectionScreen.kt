package com.caiana.talks.ui.profileselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.data.local.db.UserProfileEntity

@Composable
fun ProfileSelectionScreen(
    viewModel: ProfileSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        ProfileSelectionUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProfileSelectionUiState.ShowSelection -> {
            ProfileSelectionContent(
                profiles = (uiState as ProfileSelectionUiState.ShowSelection).profiles,
                onUserSelected = viewModel::onUserSelected
            )
        }
    }
}

@Composable
private fun ProfileSelectionContent(
    profiles: List<UserProfileEntity>,
    onUserSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quem está usando o app?",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(40.dp))
        profiles.forEach { profile ->
            Button(
                onClick = { onUserSelected(profile.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(text = profile.name, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview(showBackground = true, name = "Seleção de perfil")
@Composable
private fun ProfileSelectionContentPreview() {
    ProfileSelectionContent(
        profiles = listOf(
            UserProfileEntity(id = 1, name = "Caio"),
            UserProfileEntity(id = 2, name = "Ana")
        ),
        onUserSelected = {}
    )
}
