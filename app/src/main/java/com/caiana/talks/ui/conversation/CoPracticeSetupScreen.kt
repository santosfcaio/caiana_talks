package com.caiana.talks.ui.conversation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsButton
import com.caiana.talks.ui.theme.components.LcarsDataPanel
import com.caiana.talks.ui.theme.components.LcarsFrame
import com.caiana.talks.ui.theme.components.LcarsTopBar

@Composable
fun CoPracticeSetupScreen(
    viewModel: CoPracticeSetupViewModel = hiltViewModel(),
    onStart: (firstProfileId: Int, secondProfileId: Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LcarsFrame(accentColor = LcarsColors.Blue) {
        Column(modifier = Modifier.fillMaxSize()) {
            LcarsTopBar(title = "Conversa em dupla", accentColor = LcarsColors.Blue)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LcarsDataPanel(accentColor = LcarsColors.Blue) {
                    Text(
                        "Primeiro participante",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Text,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(state.profiles) { profile ->
                        val selected = state.firstSelectedId == profile.id
                        LcarsButton(
                            onClick = { viewModel.onSelectFirst(profile.id) },
                            color = if (selected) LcarsColors.Purple else Color.Transparent,
                            textColor = if (selected) LcarsColors.Black else LcarsColors.Blue,
                            modifier = if (!selected) Modifier.border(1.dp, LcarsColors.Blue, CircleShape)
                                       else Modifier,
                        ) {
                            Text(profile.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                LcarsDataPanel(accentColor = LcarsColors.Blue) {
                    Text(
                        "Segundo participante",
                        style = MaterialTheme.typography.titleLarge,
                        color = LcarsColors.Text,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(state.profiles) { profile ->
                        val selected = state.secondSelectedId == profile.id
                        LcarsButton(
                            onClick = { viewModel.onSelectSecond(profile.id) },
                            color = if (selected) LcarsColors.Purple else Color.Transparent,
                            textColor = if (selected) LcarsColors.Black else LcarsColors.Blue,
                            modifier = if (!selected) Modifier.border(1.dp, LcarsColors.Blue, CircleShape)
                                       else Modifier,
                        ) {
                            Text(profile.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                LcarsButton(
                    onClick = {
                        val first = state.firstSelectedId ?: return@LcarsButton
                        val second = state.secondSelectedId ?: return@LcarsButton
                        onStart(first, second)
                    },
                    enabled = state.canStart,
                    color = LcarsColors.Orange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Iniciar")
                }
            }
        }
    }
}
