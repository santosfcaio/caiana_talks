package com.caiana.talks.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caiana.talks.ui.home.HomeScreen
import com.caiana.talks.ui.main.MainViewModel
import com.caiana.talks.ui.main.StartDestination
import com.caiana.talks.ui.profileedit.ProfileEditScreen
import com.caiana.talks.ui.profileselection.ProfileSelectionScreen
import com.caiana.talks.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val startDest by mainViewModel.startDestination.collectAsStateWithLifecycle()

    when (startDest) {
        StartDestination.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        StartDestination.ProfileSelection -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "profileSelection") {
                composable("profileSelection") {
                    ProfileSelectionScreen(viewModel = hiltViewModel())
                }
            }
        }

        is StartDestination.ProfileSetup -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "profileEdit") {
                composable("profileEdit") {
                    ProfileEditScreen(
                        viewModel = hiltViewModel(),
                        onSaved = {},
                        hideBack = true
                    )
                }
            }
        }

        is StartDestination.Home -> {
            val userName = (startDest as StartDestination.Home).userName
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        userName = userName,
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSwitchProfile = { mainViewModel.clearActiveUser() },
                        onNavigateToEdit = { navController.navigate("profileEdit") }
                    )
                }
                composable("profileEdit") {
                    ProfileEditScreen(
                        viewModel = hiltViewModel(),
                        onSaved = { navController.popBackStack() },
                        hideBack = false,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
