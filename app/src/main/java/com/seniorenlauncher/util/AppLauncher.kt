package com.seniorenlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val name: String, val packageName: String, val icon: Drawable? = null)

object AppLauncher {
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("AppLauncher", "Error launching app: $packageName", e)
            false
        }
    }

    fun openSystemCamera(context: Context) {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppLauncher", "Error opening camera", e)
            Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun getInstalledApps(context: Context, includeIcons: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        apps.filter { 
            (pm.getLaunchIntentForPackage(it.packageName) != null) && it.packageName != context.packageName
        }
        .map { 
            InstalledApp(
                it.loadLabel(pm).toString(), 
                it.packageName,
                if (includeIcons) it.loadIcon(pm) else null
            ) 
        }
        .sortedBy { it.name.lowercase() }
    }

    fun getAppInfo(context: Context, packageName: String): InstalledApp? {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            InstalledApp(
                appInfo.loadLabel(pm).toString(),
                packageName,
                appInfo.loadIcon(pm)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Abre la app de mensajes predeterminada del celular (no una propia). */
    fun openMessages(context: Context, address: String? = null) {
        try {
            val intent = if (!address.isNullOrBlank()) {
                Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:" + android.net.Uri.encode(address)))
            } else {
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró la app de mensajes", Toast.LENGTH_LONG).show()
        }
    }

    fun openWifiSettings(context: Context) {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent(android.provider.Settings.Panel.ACTION_WIFI)
            } else {
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppLauncher", "Error opening wifi settings", e)
            Toast.makeText(context, "No se pudo abrir los ajustes de Wi-Fi", Toast.LENGTH_SHORT).show()
        }
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent("android.settings.panel.action.BLUETOOTH")
            } else {
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback naar volledige instellingen als panel niet ondersteund wordt
            try {
                val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("AppLauncher", "Error opening bluetooth settings", e2)
                Toast.makeText(context, "No se pudo abrir los ajustes de Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
