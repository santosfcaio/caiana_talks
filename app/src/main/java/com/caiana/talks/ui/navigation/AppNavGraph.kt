package com.caiana.talks.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.caiana.talks.ui.conversation.CoPracticeSetupScreen
import com.caiana.talks.ui.conversation.ConversationScreen
import com.caiana.talks.ui.conversation.SessionSummaryScreen
import com.caiana.talks.ui.home.HomeScreen
import com.caiana.talks.ui.main.MainViewModel
import com.caiana.talks.ui.main.StartDestination
import com.caiana.talks.ui.profileedit.ProfileEditScreen
import com.caiana.talks.ui.profileselection.ProfileSelectionScreen
import com.caiana.talks.ui.settings.SettingsScreen
import com.caiana.talks.ui.stats.StatsScreen
import com.caiana.talks.ui.theme.LcarsColors
import com.caiana.talks.ui.theme.components.LcarsProgressBar

@Composable
fun AppNavGraph(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val startDest by mainViewModel.startDestination.collectAsStateWithLifecycle()

    when (startDest) {
        StartDestination.Loading -> {
            Box(
                Modifier.fillMaxSize().background(LcarsColors.Black),
                contentAlignment = Alignment.Center,
            ) {
                LcarsProgressBar(color = LcarsColors.Orange)
            }
        }

        StartDestination.ProfileSelection -> {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "profileSelection",
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(400)) },
                popEnterTransition = { fadeIn(tween(400)) },
                popExitTransition = { fadeOut(tween(400)) },
            ) {
                composable("profileSelection") {
                    ProfileSelectionScreen(viewModel = hiltViewModel())
                }
            }
        }

        is StartDestination.ProfileSetup -> {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "profileEdit",
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(400)) },
                popEnterTransition = { fadeIn(tween(400)) },
                popExitTransition = { fadeOut(tween(400)) },
            ) {
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
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
            ) {
                composable("home") {
                    HomeScreen(
                        userName = userName,
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToStats = { navController.navigate("stats") },
                        onNavigateToConversation = { navController.navigate("conversation") },
                        onNavigateToCoPractice = { navController.navigate("coPracticeSetup") }
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
                composable("stats") {
                    StatsScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(
                    route = "conversation?secondProfileId={secondProfileId}",
                    arguments = listOf(
                        navArgument("secondProfileId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    ConversationScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToSummary = { groupId, sessionId ->
                            val route = if (groupId != null) {
                                "sessionSummary?groupId=$groupId"
                            } else {
                                "sessionSummary?sessionId=$sessionId"
                            }
                            navController.navigate(route) {
                                popUpTo("home")
                            }
                        },
                        onNavigateHome = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    )
                }
                composable("coPracticeSetup") {
                    CoPracticeSetupScreen(
                        viewModel = hiltViewModel(),
                        onStart = { _, secondProfileId ->
                            navController.navigate("conversation?secondProfileId=$secondProfileId")
                        }
                    )
                }
                composable(
                    route = "sessionSummary?sessionId={sessionId}&groupId={groupId}",
                    arguments = listOf(
                        navArgument("sessionId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("groupId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    SessionSummaryScreen(
                        viewModel = hiltViewModel(),
                        onNavigateHome = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    )
                }
            }
        }
    }
}
