package com.neurophone.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Dashboard : Screen("dashboard")
    object TeachPhone : Screen("teach_phone")
    object Memory : Screen("neural_memory")
    object Lab : Screen("neural_lab")
    object Privacy : Screen("privacy")
    object Settings : Screen("settings")
}