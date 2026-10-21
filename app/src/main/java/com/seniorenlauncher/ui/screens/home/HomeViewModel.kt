package com.seniorenlauncher.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.data.model.ALL_APPS
import com.seniorenlauncher.data.model.AppSettings
import com.seniorenlauncher.util.AppLauncher
import com.seniorenlauncher.util.DefaultApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

data class HomeState(
    val settings: AppSettings = AppSettings(),
    val allVisibleApps: List<HomeApp> = emptyList(),
    val pendingMedsCount: Int = 0
)

/** Botones que muestran la app real del telefono (icono y color propios). */
private val REAL_APP_IDS = setOf("photos", "browser", "camera", "sms", "email", "settings", "weather")

/** Posicion fija de "Mis apps": arriba a la derecha (primera fila, segunda columna). */
private const val MY_APPS_SLOT = 1

class HomeViewModel : ViewModel() {
    private val repository = LauncherApp.instance.settingsRepository
    private val medicationDao = LauncherApp.instance.database.medicationDao()

    val state: StateFlow<HomeState> = combine(
        repository.settingsFlow,
        medicationDao.getPending()
    ) { settings, pendingMeds ->
        HomeState(
            settings = settings,
            pendingMedsCount = pendingMeds.size,
            allVisibleApps = calculateVisibleApps(settings)
        )
    }
        .flowOn(Dispatchers.Default) // leer iconos y colores fuera del hilo de la pantalla
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private fun calculateVisibleApps(settings: AppSettings): List<HomeApp> {
        val context = LauncherApp.instance.applicationContext

        val standardApps = ALL_APPS
            .filter { it.id in settings.visibleApps && it.id != "all_apps" }
            .map { app ->
                val realPkg = if (app.id in REAL_APP_IDS) DefaultApps.packageFor(context, app.id) else null
                val realIcon = realPkg?.let { DefaultApps.icon(context, it) }
                if (realPkg != null && realIcon != null) {
                    HomeApp(
                        id = app.id,
                        name = app.name,
                        icon = realIcon,
                        color = DefaultApps.mainColor(realPkg, realIcon) ?: Color(app.color)
                    )
                } else {
                    HomeApp(
                        id = app.id,
                        name = app.name,
                        emoji = app.emoji,
                        vectorIcon = when (app.id) {
                            "bluetooth" -> Icons.Default.Bluetooth
                            "wifi" -> Icons.Default.Wifi
                            "power" -> Icons.Default.PowerSettingsNew
                            else -> null
                        },
                        color = Color(app.color)
                    )
                }
            }

        val mappedApps = settings.appMappings
            .filterKeys { it.startsWith("mapped_") && it in settings.visibleApps }
            .map { (id, pkg) ->
                val info = AppLauncher.getAppInfo(context, pkg)
                HomeApp(
                    id = id,
                    name = info?.name ?: "App",
                    icon = info?.icon,
                    color = info?.icon?.let { DefaultApps.mainColor(pkg, it) } ?: Color(0xFF475569)
                )
            }

        val myApps = ALL_APPS.first { it.id == "all_apps" }.let {
            HomeApp(id = it.id, name = it.name, emoji = it.emoji, color = Color(it.color))
        }
        val apps = (standardApps + mappedApps).distinctBy { it.id }.toMutableList()
        apps.add(MY_APPS_SLOT.coerceAtMost(apps.size), myApps)
        return apps
    }
}
