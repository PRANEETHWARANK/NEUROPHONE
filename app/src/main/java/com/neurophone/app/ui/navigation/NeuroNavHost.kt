package com.neurophone.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neurophone.app.NeuroViewModel
import com.neurophone.app.ui.dashboard.AiDashboardScreen
import com.neurophone.app.ui.home.HomeScreen
import com.neurophone.app.ui.lab.NeuralLabScreen
import com.neurophone.app.ui.memory.NeuralMemoryScreen
import com.neurophone.app.ui.privacy.PrivacyCenterScreen
import com.neurophone.app.ui.teach.TeachPhoneScreen

@Composable
fun NeuroNavHost(viewModel: NeuroViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = uiState,
                onAppClick = { app -> viewModel.onAppLaunched(app) },
                onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToTeach = { navController.navigate(Screen.TeachPhone.route) },
                onNavigateToMemory = { navController.navigate(Screen.Memory.route) },
                onNavigateToLab = { navController.navigate(Screen.Lab.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
            )
        }

        composable(Screen.Dashboard.route) {
            AiDashboardScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TeachPhone.route) {
            TeachPhoneScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Memory.route) {
            NeuralMemoryScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Lab.route) {
            NeuralLabScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyCenterScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}