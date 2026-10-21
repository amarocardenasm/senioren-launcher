package com.seniorenlauncher.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Intent
import android.content.IntentFilter
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
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(Date())) }
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(now)

            val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // Actualiza justo al cambiar el minuto
            kotlinx.coroutines.delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Fila chica: bateria y avisos a la izquierda, emergencia a la derecha
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val lowBattery = batteryLevel in 0..19 && !isCharging
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (lowBattery) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                    contentDescription = "Batería",
                    modifier = Modifier.size(24.dp),
                    tint = if (lowBattery) Color(0xFFEF4444) else Color(0xFF10B981)
                )
                Spacer(Modifier.width(4.dp))
                Text("$batteryLevel%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(Modifier.width(16.dp))

            // Solo la campanita; el globo rojo queda por fuera para que se vea completo
            Box(
                Modifier
                    .size(56.dp)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(containerColor = Color(0xFFEF4444), contentColor = Color.White) {
                                Text(if (notificationCount > 99) "99+" else "$notificationCount", fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Avisos",
                        modifier = Modifier.size(34.dp),
                        tint = if (notificationCount > 0) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Boton de emergencia pequeño: hay que mantenerlo presionado
            EmergencyHoldChip(onActivate = onEmergencyHold)
        }

        // La hora es la protagonista
        Column(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                currentTime,
                fontSize = 84.sp,
                fontWeight = FontWeight.W900,
                color = Color.White,
                letterSpacing = (-2).sp,
                lineHeight = 88.sp
            )
            Text(
                currentDate.replaceFirstChar { it.uppercase() },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA5B4FC)
            )
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

