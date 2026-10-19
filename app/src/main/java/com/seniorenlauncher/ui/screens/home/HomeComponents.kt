package com.seniorenlauncher.ui.screens.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorenlauncher.ui.theme.SeniorenLauncherTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeTopBar(notificationCount: Int, onNotificationsClick: () -> Unit, onEmergencyHold: () -> Unit) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date())) }
    
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var isWifiConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)
            
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            kotlinx.coroutines.delay(60000)
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
        // Boton de emergencia pequeño y arriba: hay que mantenerlo presionado
        Box(
            Modifier
                .fillMaxWidth()
                .padding(end = 24.dp, top = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            EmergencyHoldChip(onActivate = onEmergencyHold)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Links: Status Capsule (Glass Effect)
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                modifier = Modifier.height(76.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (batteryLevel < 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (batteryLevel < 20 && !isCharging) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Text(
                        text = "$batteryLevel%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                    Icon(
                        imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Midden: Elegante Klok (Hyper-Refined)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    currentTime, 
                    fontSize = 62.sp, 
                    fontWeight = FontWeight.W900,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    currentDate.uppercase(), 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF6366F1), // Modern Indigo
                    letterSpacing = 2.5.sp
                )
            }

            // Rechts: Notification Avatar (Glass Effect)
            Surface(
                onClick = onNotificationsClick,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notifications, 
                        contentDescription = "Avisos",
                        modifier = Modifier.size(34.dp),
                        tint = if (notificationCount > 0) Color(0xFF6366F1) else Color.White.copy(alpha = 0.4f)
                    )
                    if (notificationCount > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp),
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            border = BorderStroke(2.dp, Color(0xFF0F172A))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$notificationCount",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * Boton de emergencia discreto. Un toque corto solo muestra un aviso;
 * hay que mantenerlo presionado 2 segundos para abrir la pantalla de emergencias.
 */
@Composable
fun EmergencyHoldChip(onActivate: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val released = withTimeoutOrNull(2000L) { tryAwaitRelease() }
                        if (released == null) {
                            onActivate()
                        } else {
                            Toast.makeText(
                                context,
                                "Mantenga presionado 2 segundos para emergencias",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF7F1D1D).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.HealthAndSafety,
                contentDescription = "Emergencia",
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(6.dp))
            Text("EMERGENCIA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun HomeStatusCard(
    pendingMedsCount: Int,
    onMedsClick: () -> Unit
) {
    Surface(
        onClick = onMedsClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(28.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(64.dp).background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💊", fontSize = 36.sp)
            }
            Spacer(Modifier.width(24.dp))
            Column {
                Text(
                    "RECORDATORIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF10B981)
                )
                Text(
                    "Faltan $pendingMedsCount medicamentos", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

fun getHomeWeatherEmoji(iconUrl: String): String {
    return when {
        iconUrl.contains("01d") -> "☀️"
        iconUrl.contains("01n") -> "🌙"
        iconUrl.contains("02d") || iconUrl.contains("02n") -> "⛅"
        iconUrl.contains("03") || iconUrl.contains("04") -> "☁️"
        iconUrl.contains("09") || iconUrl.contains("10") -> "🌧️"
        iconUrl.contains("11") -> "⛈️"
        iconUrl.contains("13") -> "❄️"
        iconUrl.contains("50") -> "🌫️"
        else -> "☁️"
    }
}
