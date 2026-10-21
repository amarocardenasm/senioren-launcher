package com.seniorenlauncher.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.service.FallDetectionService
import com.seniorenlauncher.util.AlarmScheduler
import com.seniorenlauncher.util.MedicationAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = LauncherApp.instance
                    val db = app.database
                    val settings = app.settingsRepository.settingsFlow.first()

                    // 0. Start Fall Detection if enabled and permissions granted
                    if (settings.fallDetectionEnabled) {
                        ctx.startForegroundService(Intent(ctx, FallDetectionService::class.java))
                    }

                    // 1. Medication alarms
                    val medications = db.medicationDao().getAllSync()
                    medications.forEach { med ->
                        if (med.active) {
                            MedicationAlarmScheduler.scheduleAlarms(ctx, med)
                        }
                    }
                    
                    // 2. Normal alarms
                    val alarms = db.alarmDao().getAllSync()
                    alarms.forEach { alarm ->
                        if (alarm.enabled) {
                            AlarmScheduler.scheduleAlarm(ctx, alarm)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
