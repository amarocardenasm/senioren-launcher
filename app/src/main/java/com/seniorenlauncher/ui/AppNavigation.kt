package com.seniorenlauncher.ui

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.seniorenlauncher.ui.screens.*

@Composable
fun AppNavigation(
    settingsVm: SettingsViewModel,
    initialWeatherNav: Boolean = false,
    onNavigatedToWeather: () -> Unit = {},
    onRouteChanged: (String?) -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            onRouteChanged(entry.destination.route)
        }
    }

    LaunchedEffect(initialWeatherNav) {
        if (initialWeatherNav) {
            navController.navigate("weather")
            onNavigatedToWeather()
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                settingsVm = settingsVm
            )
        }
        composable("phone") { 
            PhoneScreen(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("camera") { /* Handled in HomeScreen via AppLauncher */ }
        composable("photos") { PhotosScreen(onBack = { navController.popBackStack() }) }
        composable("alarm") { AlarmScreen(onBack = { navController.popBackStack() }) }
        composable("calendar") { CalendarScreen(onBack = { navController.popBackStack() }) }
        composable("meds") { MedicationScreen(onBack = { navController.popBackStack() }) }
        composable("weather") { WeatherScreen(onBack = { navController.popBackStack() }) }
        composable("flashlight") { FlashlightScreen(onBack = { navController.popBackStack() }) }
        composable("magnifier") { MagnifierScreen(onBack = { navController.popBackStack() }) }
        composable("notes") { NotesScreen(onBack = { navController.popBackStack() }) }
        composable("steps") { StepsScreen(onBack = { navController.popBackStack() }) }
        composable("emergency") { EmergencyInfoScreen(onBack = { navController.popBackStack() }) }
        composable("sos") { SOSScreen(onBack = { navController.popBackStack() }) }
        composable("sos_settings") { SosContactSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("privacy") { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable("all_apps") { AllAppsScreen(onBack = { navController.popBackStack() }) }
        composable("settings") { 
            SettingsScreen(
                vm = settingsVm, 
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            ) 
        }
        composable("notifications") { 
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            ) 
        }
    }
}
