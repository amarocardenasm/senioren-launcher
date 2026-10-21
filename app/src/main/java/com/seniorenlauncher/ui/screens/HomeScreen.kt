package com.seniorenlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorenlauncher.service.NotificationListener
import com.seniorenlauncher.ui.components.BigButton
import com.seniorenlauncher.ui.screens.home.*
import com.seniorenlauncher.util.AppLauncher
import com.seniorenlauncher.util.DefaultApps
import com.seniorenlauncher.util.Torch

/** 2 columnas, igual que "Mis apps": botones grandes y cuadrados que se recorren hacia abajo. */
private const val COLUMNS = 2
private const val ADD_ID = "__add__"

/** Botones que abren otra app (muestran "Abriendo…" mientras carga). */
private val OPENS_OTHER_APP = setOf("sms", "camera", "photos", "browser", "email", "settings", "weather", "wifi", "bluetooth")

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, settingsVm: SettingsViewModel) {
    val context = LocalContext.current
    val homeVm: HomeViewModel = viewModel()
    val uiState by homeVm.state.collectAsState()
    val activeNotifications by NotificationListener.activeNotificationsFlow.collectAsState()
    val badgeCounts by NotificationListener.notifications.collectAsState()

    LaunchedEffect(Unit) { Torch.init(context) }
    val torchOn by Torch.isOn.collectAsState()

    var showAppPicker by remember { mutableStateOf(false) }
    var appToRemove by remember { mutableStateOf<HomeApp?>(null) }

    val apps = uiState.allVisibleApps
    val slots = apps + HomeApp(id = ADD_ID, name = "Agregar", emoji = "➕", color = Color(0xFF334155))

    fun open(app: HomeApp) {
        when {
            app.id == ADD_ID -> showAppPicker = true
            app.id == "phone" -> onNavigate("phone")
            app.id == "sms" -> AppLauncher.openMessages(context)
            app.id == "camera" -> if (!DefaultApps.open(context, "camera")) AppLauncher.openSystemCamera(context)
            app.id == "photos" -> if (!DefaultApps.open(context, "photos")) AppLauncher.openGallery(context)
            app.id == "browser" -> if (!DefaultApps.open(context, "browser")) AppLauncher.openBrowser(context)
            app.id == "email" -> DefaultApps.open(context, "email")
            app.id == "settings" -> DefaultApps.open(context, "settings")
            app.id == "weather" -> AppLauncher.openWeather(context)
            app.id == "wifi" -> AppLauncher.openWifiSettings(context)
            app.id == "bluetooth" -> AppLauncher.openBluetoothSettings(context)
            app.id == "flashlight" -> Torch.toggle(context)
            app.id.startsWith("mapped_") -> uiState.settings.appMappings[app.id]?.let { AppLauncher.launchApp(context, it) }
            else -> onNavigate(app.id)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        HomeTopBar(
            notificationCount = activeNotifications.size,
            onNotificationsClick = { onNavigate("notifications") },
            onEmergencyHold = { onNavigate("emergency_numbers") }
        )

        if (uiState.pendingMedsCount > 0) {
            HomeStatusCard(
                pendingMedsCount = uiState.pendingMedsCount,
                onMedsClick = { onNavigate("meds") }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(slots, key = { it.id }) { app ->
                val isTorch = app.id == "flashlight"
                BigButton(
                    emoji = if (isTorch && torchOn) "💡" else app.emoji,
                    icon = app.icon,
                    vectorIcon = app.vectorIcon,
                    label = if (isTorch && torchOn) "Apagar linterna" else app.name,
                    color = if (isTorch && torchOn) Color(0xFFF59E0B) else app.color,
                    badge = if (app.id == "sms" || app.id == "phone") {
                        NotificationListener.getBadgeCountByAppId(app.id, uiState.settings.appMappings, badgeCounts)
                    } else 0,
                    opensOtherApp = app.id in OPENS_OTHER_APP || app.id.startsWith("mapped_"),
                    onClick = { open(app) },
                    // "Mis apps" y "Agregar" no se pueden quitar
                    onLongClick = if (app.id == "all_apps" || app.id == ADD_ID) null else {
                        { appToRemove = app }
                    },
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppsSelected = { pkgs ->
                settingsVm.addAppMappingsBulk(
                    pkgs.associateBy { pkg -> "mapped_${System.currentTimeMillis()}_${pkg.hashCode()}" }
                )
                showAppPicker = false
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
}
