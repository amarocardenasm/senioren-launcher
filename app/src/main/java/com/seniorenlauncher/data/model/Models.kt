package com.seniorenlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppTheme { CLASSIC, HIGH_CONTRAST, LIGHT }
enum class LayoutType { GRID_2x3, GRID_3x4, GRID_1x1 }

data class AppSettings(
    val theme: AppTheme = AppTheme.CLASSIC,
    val layout: LayoutType = LayoutType.GRID_2x3,
    val fontSize: Int = 18,
    val language: String = "es",
    val nightModeAuto: Boolean = true,
    val visibleApps: Set<String> = setOf("phone", "sms", "camera", "photos", "browser", "email", "flashlight", "settings", "meds"),
    val appMappings: Map<String, String> = emptyMap(),
    val settingsLocked: Boolean = false,
    val pinCode: String? = null,
    val fallDetectionEnabled: Boolean = false,
    val batteryAlertEnabled: Boolean = true,
    val chargingReminderEnabled: Boolean = true,
    val scamProtectionEnabled: Boolean = false,
    val hasCompletedSetup: Boolean = false,
    val privacyAccepted: Boolean = false,
    val userPhoneNumber: String? = null,
    val stepsToday: Int = 0
)

@Entity(tableName = "contacts")
data class QuickContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val emoji: String = "👤",
    val color: Long = 0xFF718096,
    val isSosContact: Boolean = false,
    val sortOrder: Int = 0,
    val photoUri: String? = null
)


@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val times: String, 
    val daysOfWeek: String = "1,2,3,4,5,6,7",
    val isTaken: Boolean = false,
    val lastTakenDate: Long = 0,
    val active: Boolean = true,
    val isPending: Boolean = false,
    val stockCount: Int = 0,
    val lowStockThreshold: Int = 5,
    val photoUri: String? = null,
    val lastActionTime: Long = 0
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val date: Long,
    val time: String,
    val status: String
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTime: Long,
    val description: String = ""
)

@Entity(tableName = "emergency_info")
data class EmergencyInfo(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val birthDate: String = "",
    val address: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val hasPacemaker: Boolean = false,
    val doctorName: String = "",
    val doctorPhone: String = "",
    val iceContactName: String = "",
    val iceContactPhone: String = ""
)

@Entity(tableName = "alarms")
data class AlarmEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val daysOfWeek: String = "",
    val enabled: Boolean = true,
    val soundUri: String? = null,
    val isMorningRoutine: Boolean = false
)

data class AppInfo(
    val id: String,
    val name: String,
    val icon: String,
    val emoji: String,
    val color: Long
)

// El orden de esta lista es el orden en el inicio (cuadricula de 3x3).
// "Mis apps" siempre va fija arriba a la derecha de la primera pagina.
val ALL_APPS = listOf(
    AppInfo("phone", "Teléfono", "📞", "📞", 0xFF15803D),
    AppInfo("sms", "Mensajes", "💬", "💬", 0xFF2563EB),
    AppInfo("camera", "Cámara", "📷", "📷", 0xFFBE185D),
    AppInfo("photos", "Fotos", "🖼️", "🖼️", 0xFFB45309),
    AppInfo("browser", "Internet", "🌐", "🌐", 0xFF1D4ED8),
    AppInfo("email", "Correo", "✉️", "✉️", 0xFFB91C1C),
    AppInfo("flashlight", "Linterna", "🔦", "🔦", 0xFF475569),
    AppInfo("settings", "Ajustes", "⚙️", "⚙️", 0xFF475569),
    AppInfo("meds", "Medicamentos", "💊", "💊", 0xFFB91C1C),
    AppInfo("alarm", "Alarma", "⏰", "⏰", 0xFFC2410C),
    AppInfo("calendar", "Agenda", "📅", "📅", 0xFF0F766E),
    AppInfo("weather", "Clima", "🌤️", "🌤️", 0xFF0369A1),
    AppInfo("magnifier", "Lupa", "🔍", "🔍", 0xFF4F46E5),
    AppInfo("notes", "Notas", "📝", "📝", 0xFF4D7C0F),
    AppInfo("wifi", "Wi-Fi", "📶", "📶", 0xFF475569),
    AppInfo("bluetooth", "Bluetooth", "🔵", "🔵", 0xFF1D4ED8),
    AppInfo("steps", "Pasos", "🚶", "🚶", 0xFF0F766E),
    AppInfo("emergency", "Ficha médica", "🏥", "🏥", 0xFFB91C1C),
    AppInfo("all_apps", "Mis apps", "📱", "📱", 0xFF334155),
)
