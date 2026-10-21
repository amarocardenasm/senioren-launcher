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

    /** Abre la galeria de fotos del telefono (no necesita permisos de fotos). */
    fun openGallery(context: Context) {
        val gallery = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_GALLERY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(gallery)
            return
        } catch (e: Exception) {
            Log.w("AppLauncher", "Sin galeria predeterminada", e)
        }
        // Galerias conocidas: Samsung, Google Fotos, Xiaomi, Motorola/AOSP
        val known = listOf(
            "com.sec.android.gallery3d",
            "com.google.android.apps.photos",
            "com.miui.gallery",
            "com.android.gallery3d"
        )
        for (pkg in known) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        Toast.makeText(context, "No se encontró la app de galería", Toast.LENGTH_SHORT).show()
    }

    fun openBrowser(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.cl"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró un navegador", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun getInstalledApps(context: Context, includeIcons: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // Orden alfabetico en español (ignora tildes y mayusculas)
        val collator = java.text.Collator.getInstance(java.util.Locale("es", "CL")).apply {
            strength = java.text.Collator.PRIMARY
        }
        pm.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                InstalledApp(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    if (includeIcons) it.loadIcon(pm) else null
                )
            }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
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
