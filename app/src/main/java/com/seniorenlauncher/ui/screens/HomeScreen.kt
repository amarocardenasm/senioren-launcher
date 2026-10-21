package com.seniorenlauncher.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.data.model.*
import com.seniorenlauncher.service.NotificationListener
import com.seniorenlauncher.ui.components.*
import com.seniorenlauncher.ui.screens.home.*
import com.seniorenlauncher.ui.theme.SeniorenLauncherTheme
import com.seniorenlauncher.util.AppLauncher
import com.seniorenlauncher.util.DefaultApps
import com.seniorenlauncher.util.Torch
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit, settingsVm: SettingsViewModel) {
    val context = LocalContext.current
    val homeVm: HomeViewModel = viewModel()
    val uiState by homeVm.state.collectAsState()
    
    val activeNotifications by NotificationListener.activeNotificationsFlow.collectAsState()
    val badgeCounts by NotificationListener.notifications.collectAsState()
    
    val weatherVm: WeatherViewModel = viewModel()
    val weatherData by weatherVm.currentWeather.collectAsState()
    
    // Sync weather data to homeViewModel
    LaunchedEffect(weatherData) {
        homeVm.setWeatherData(weatherData)
    }

    var showAppPickerFor by remember { mutableStateOf<String?>(null) }
    var appToRemove by remember { mutableStateOf<HomeApp?>(null) }
    var showPinDialogForSettings by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var showBluetoothDialog by remember { mutableStateOf(false) }

    HomeScreenContent(
        settings = uiState.settings,
        allVisibleApps = uiState.allVisibleApps,
        activeNotificationsCount = activeNotifications.size,
        badgeCounts = badgeCounts,
        pendingMedsCount = uiState.pendingMedsCount,
        onNavigate = onNavigate,
        onAddApp = { showAppPickerFor = "new" },
        onAppLongClick = { id -> appToRemove = uiState.allVisibleApps.firstOrNull { it.id == id } },
        onSettingsClick = {
            // Solo pide clave si alguien (un familiar) la configuró
            if (uiState.settings.pinCode.isNullOrBlank()) onNavigate("settings")
            else showPinDialogForSettings = true
        },
        onWifiClick = { showWifiDialog = true },
        onBluetoothClick = { showBluetoothDialog = true },
        onEmergencyHold = { onNavigate("emergency_numbers") }
    )

    if (showAppPickerFor != null) {
        val pickerAppId = showAppPickerFor!!
        AppPickerDialog(
            appId = pickerAppId,
            onDismiss = { showAppPickerFor = null },
            onAppsSelected = { pkgs: List<String> ->
                val newMappings = mutableMapOf<String, String>()
                pkgs.forEach { pkg ->
                    val newId = "mapped_${System.currentTimeMillis()}_${pkg.hashCode()}"
                    newMappings[newId] = pkg
                }
                settingsVm.addAppMappingsBulk(newMappings)
                showAppPickerFor = null
            },
            onRemove = {
                settingsVm.updateVisibleApps(uiState.settings.visibleApps - pickerAppId)
                showAppPickerFor = null
            }
        )
    }
    
    appToRemove?.let { app ->
        AlertDialog(
            onDismissRequest = { appToRemove = null },
            title = { Text("¿Quitar ${app.name} de la pantalla de inicio?", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
            text = { Text("La app no se borra del teléfono. Puede volver a ponerla con el botón Agregar.", fontSize = 18.sp) },
            confirmButton = {
                Button(onClick = {
                    settingsVm.updateVisibleApps(uiState.settings.visibleApps - app.id)
                    appToRemove = null
                }) { Text("QUITAR", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { appToRemove = null }) { Text("CANCELAR", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showPinDialogForSettings) {
        PinDialog(
            correctPin = uiState.settings.pinCode ?: "",
            onDismiss = { showPinDialogForSettings = false },
            onSuccess = { 
                showPinDialogForSettings = false
                onNavigate("settings")
            }
        )
    }

    if (showWifiDialog) {
        WifiDialog(onDismiss = { showWifiDialog = false })
    }

    if (showBluetoothDialog) {
        BluetoothDialog(onDismiss = { showBluetoothDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    settings: AppSettings,
    allVisibleApps: List<HomeApp>,
    activeNotificationsCount: Int,
    badgeCounts: Map<String, Int>,
    pendingMedsCount: Int,
    onNavigate: (String) -> Unit,
    onAddApp: () -> Unit,
    onAppLongClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onWifiClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onEmergencyHold: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Torch.init(context) }
    val torchOn by Torch.isOn.collectAsState()
    
    // --- Dynamic Layout Logic ---
    val cols = when (settings.layout) {
        LayoutType.GRID_1x1 -> 1
        LayoutType.GRID_2x3 -> 2
        LayoutType.GRID_3x4 -> 3
    }
    
    val appsPerPage = when (settings.layout) {
        LayoutType.GRID_1x1 -> 1
        LayoutType.GRID_2x3 -> 6
        LayoutType.GRID_3x4 -> 12
    }

    // Bereken het totaal aantal items inclusief de "Agregar" knop
    val totalItemsCount = allVisibleApps.size + 1
    val pageCount = max(1, ceil(totalItemsCount.toDouble() / appsPerPage.toDouble()).toInt())
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Bar: Clock, Battery & Notifications
            HomeTopBar(
                notificationCount = activeNotificationsCount,
                onNotificationsClick = { onNavigate("notifications") },
                onEmergencyHold = onEmergencyHold
            )

            // Status Card: Alleen nog voor Medicijnen
            if (pendingMedsCount > 0) {
                HomeStatusCard(
                    pendingMedsCount = pendingMedsCount,
                    onMedsClick = { onNavigate("meds") }
                )
            }

            // App Grid Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                pageSpacing = 32.dp
            ) { pageIndex ->
            val startIdx = pageIndex * appsPerPage
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                userScrollEnabled = false
            ) {
                // Apps voor deze pagina
                val endIdx = Math.min(startIdx + appsPerPage, allVisibleApps.size)
                if (startIdx < allVisibleApps.size) {
                    val pageApps = allVisibleApps.subList(startIdx, endIdx)
                    items(pageApps) { app ->
                        val isTorch = app.id == "flashlight"
                        BigButton(
                            emoji = if (isTorch && torchOn) "💡" else app.emoji,
                            icon = app.icon,
                            vectorIcon = app.vectorIcon,
                            label = if (isTorch && torchOn) "Apagar linterna" else app.name,
                            color = if (isTorch) (if (torchOn) Color(0xFFD97706) else Color(0xFF475569)) else app.color,
                            small = settings.layout == LayoutType.GRID_3x4,
                            badge = if (app.id == "sms" || app.id == "phone") {
                               NotificationListener.getBadgeCountByAppId(app.id, settings.appMappings, badgeCounts)
                            } else 0,
                            weatherText = app.weatherOverlay,
                            onClick = {
                                when {
                                    app.id == "camera" -> if (!DefaultApps.open(context, "camera")) AppLauncher.openSystemCamera(context)
                                    app.id == "photos" -> if (!DefaultApps.open(context, "photos")) AppLauncher.openGallery(context)
                                    app.id == "browser" -> if (!DefaultApps.open(context, "browser")) AppLauncher.openBrowser(context)
                                    app.id == "flashlight" -> Torch.toggle(context)
                                    app.id == "wifi" -> onWifiClick()
                                    app.id == "bluetooth" -> onBluetoothClick()
                                    app.id == "sms" -> AppLauncher.openMessages(context)
                                    app.id == "settings" -> onSettingsClick()
                                    app.id.startsWith("mapped_") -> {
                                        val pkg = settings.appMappings[app.id]
                                        if (pkg != null) AppLauncher.launchApp(context, pkg)
                                    }
                                    else -> onNavigate(app.id)
                                }
                            },
                            onLongClick = {
                                if (app.id != "settings" && app.id != "all_apps") {
                                    onAppLongClick(app.id)
                                }
                            }
                        )
                    }
                }
                
                // Voeg de "Agregar" knop toe als we op de juiste pagina zijn
                val addBtnIdx = allVisibleApps.size
                val start = startIdx
                val limit = startIdx + appsPerPage
                
                if (addBtnIdx in start until limit) {
                    item {
                        BigButton(
                            emoji = "➕",
                            label = "Agregar",
                            color = Color(0xFF718096),
                            small = settings.layout == LayoutType.GRID_3x4,
                            onClick = onAddApp
                        )
                    }
                }
            }
        }
    }
}
}

@Preview(showSystemUi = true)
@Composable
fun PreviewHomeScreen() {
    SeniorenLauncherTheme {
        HomeScreenContent(
            settings = AppSettings(
                layout = LayoutType.GRID_2x3,
                visibleApps = setOf("phone", "sms", "camera", "photos", "meds", "weather", "wifi")
            ),
            allVisibleApps = emptyList(),
            activeNotificationsCount = 2,
            badgeCounts = mapOf("sms" to 1),
            pendingMedsCount = 1,
            onNavigate = {},
            onAddApp = {},
            onAppLongClick = {},
            onSettingsClick = {},
            onWifiClick = {},
            onBluetoothClick = {},
            onEmergencyHold = {}
        )
    }
}
