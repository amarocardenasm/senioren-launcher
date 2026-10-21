package com.seniorenlauncher.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.data.model.QuickContact
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SOSService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tag = "SOSService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, LauncherApp.CH_SOS)
            .setContentTitle("ALERTA DE EMERGENCIA ACTIVA")
            .setContentText("Se está llamando a su contacto de emergencia.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1001, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }

        scope.launch {
            try {
                processSOS()
            } catch (e: Exception) {
                Log.e(tag, "Error al ejecutar la emergencia", e)
            } finally {
                // We stoppen de service niet direct, zodat de intercom-modus actief kan blijven
                delay(10000)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processSOS() {
        val context = applicationContext
        val dao = LauncherApp.instance.database.contactDao()
        val sosContacts = dao.getSosContacts().first()

        if (sosContacts.isEmpty()) {
            Log.w(tag, "No se encontraron contactos de emergencia")
            return
        }

        // Llamar directamente al primer contacto SOS (ya no se envian SMS)
        val firstContact = sosContacts.first()
        makeEmergencyCall(firstContact.phoneNumber)
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
