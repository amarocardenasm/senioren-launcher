package com.seniorenlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AColor

/**
 * Encuentra la app que el telefono usa por defecto para cada funcion
 * (fotos, internet, camara, mensajes), para mostrar en el inicio
 * su icono real y abrirla directamente.
 */
object DefaultApps {

    private val colorCache = mutableMapOf<String, Color>()

    /** Paquete de la app predeterminada para un boton del inicio, o null si no hay. */
    fun packageFor(context: Context, appId: String): String? {
        val pm = context.packageManager
        return when (appId) {
            "photos" -> firstInstalled(pm, "com.google.android.apps.photos")
                ?: resolve(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
                ?: firstInstalled(pm, "com.sec.android.gallery3d", "com.miui.gallery", "com.android.gallery3d")
            "browser" -> resolve(pm, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addCategory(Intent.CATEGORY_BROWSABLE))
                ?: resolve(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
                ?: firstInstalled(pm, "com.android.chrome", "com.sec.android.app.sbrowser", "org.mozilla.firefox")
            "camera" -> resolve(pm, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                ?: resolve(pm, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            "sms" -> Telephony.Sms.getDefaultSmsPackage(context)
            "email" -> firstInstalled(pm, "com.google.android.gm")
                ?: resolve(pm, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL))
                ?: firstInstalled(pm, "com.samsung.android.email.provider", "com.microsoft.office.outlook")
            "settings" -> resolve(pm, Intent(Settings.ACTION_SETTINGS)) ?: "com.android.settings"
            // No existe una "app de clima predeterminada" en Android: usamos la del fabricante
            "weather" -> firstInstalled(
                pm,
                "com.sec.android.daemonapp",      // Samsung
                "com.miui.weather2",              // Xiaomi
                "com.motorola.timeweatherwidget", // Motorola
                "com.huawei.android.totemweather" // Huawei
            )
            else -> null
        }
    }

    /** Abre la app predeterminada; devuelve false si no se pudo. */
    fun open(context: Context, appId: String): Boolean {
        if (appId == "settings") return start(context, Intent(Settings.ACTION_SETTINGS))
        val pkg = packageFor(context, appId) ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun icon(context: Context, pkg: String): Drawable? = try {
        context.packageManager.getApplicationIcon(pkg)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * Color principal de un icono (ej. WhatsApp -> verde), oscurecido lo
     * justo para que el texto blanco se lea bien. Null si el icono es gris.
     */
    fun mainColor(pkg: String, drawable: Drawable): Color? {
        colorCache[pkg]?.let { return it }
        val size = 32
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        // Agrupa los pixeles con color en 12 tonos y se queda con el mas frecuente
        val buckets = Array(12) { FloatArray(4) } // r, g, b, peso
        val hsv = FloatArray(3)
        for (x in 0 until size) for (y in 0 until size) {
            val p = bmp.getPixel(x, y)
            if (AColor.alpha(p) < 160) continue
            AColor.colorToHSV(p, hsv)
            if (hsv[1] < 0.35f || hsv[2] < 0.25f) continue
            val b = ((hsv[0] / 30f).toInt()).coerceIn(0, 11)
            val w = hsv[1] * hsv[2]
            buckets[b][0] += AColor.red(p) * w
            buckets[b][1] += AColor.green(p) * w
            buckets[b][2] += AColor.blue(p) * w
            buckets[b][3] += w
        }
        bmp.recycle()
        val best = buckets.maxByOrNull { it[3] } ?: return null
        if (best[3] < 8f) return null
        val rgb = AColor.rgb(
            (best[0] / best[3]).toInt(),
            (best[1] / best[3]).toInt(),
            (best[2] / best[3]).toInt()
        )
        AColor.colorToHSV(rgb, hsv)
        hsv[2] = hsv[2].coerceAtMost(0.62f)
        hsv[1] = hsv[1].coerceAtMost(0.85f)
        val color = Color(AColor.HSVToColor(hsv))
        colorCache[pkg] = color
        return color
    }

    private fun resolve(pm: PackageManager, intent: Intent): String? {
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val pkg = info.activityInfo?.packageName ?: return null
        // "android" es la ventana de "elegir app": no hay predeterminada
        return if (pkg == "android" || pkg.contains("resolver")) null else pkg
    }

    private fun firstInstalled(pm: PackageManager, vararg pkgs: String): String? =
        pkgs.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
}
