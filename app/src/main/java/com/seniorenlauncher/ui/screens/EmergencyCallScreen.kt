package com.seniorenlauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorenlauncher.LauncherApp
import com.seniorenlauncher.ui.components.ScreenHeader

private data class EmergencyService(
    val number: String,
    val name: String,
    val description: String,
    val emoji: String,
    val color: Color
)

/**
 * Pantalla de emergencias para Chile.
 * Cada llamada pide una confirmación aparte antes de marcar, para evitar llamadas accidentales.
 */
@Composable
fun EmergencyCallScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val contactDao = remember { LauncherApp.instance.database.contactDao() }
    val sosContacts by contactDao.getSosContacts().collectAsState(initial = emptyList())

    // Pareja (numero, nombre) pendiente de confirmar
    var pendingCall by remember { mutableStateOf<Pair<String, String>?>(null) }

    val services = listOf(
        EmergencyService("131", "Ambulancia", "Urgencia médica", "🚑", Color(0xFFDC2626)),
        EmergencyService("132", "Bomberos", "Incendio o rescate", "🚒", Color(0xFFEA580C)),
        EmergencyService("133", "Carabineros", "Policía, robo o peligro", "🚓", Color(0xFF2563EB))
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(title = "Emergencias", onBack = onBack)

            Text(
                "Toque el servicio que necesita. Antes de llamar se le pedirá confirmar.",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(20.dp))

            services.forEach { service ->
                EmergencyCard(
                    emoji = service.emoji,
                    title = service.name,
                    subtitle = service.description,
                    trailing = service.number,
                    color = service.color,
                    onClick = { pendingCall = service.number to service.name }
                )
                Spacer(Modifier.height(14.dp))
            }

            val firstContact = sosContacts.firstOrNull()
            if (firstContact != null) {
                EmergencyCard(
                    emoji = "📞",
                    title = "Mi contacto de emergencia",
                    subtitle = firstContact.name,
                    trailing = "",
                    color = Color(0xFF10B981),
                    onClick = { pendingCall = firstContact.phoneNumber to firstContact.name }
                )
                Spacer(Modifier.height(14.dp))
            }

            EmergencyCard(
                emoji = "🏥",
                title = "Mi ficha médica",
                subtitle = "Datos de salud para el personal de emergencia",
                trailing = "",
                color = Color(0xFF6366F1),
                onClick = { onNavigate("emergency") }
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    pendingCall?.let { call ->
        AlertDialog(
            onDismissRequest = { pendingCall = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("¿Llamar a ${call.second}?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
            },
            text = {
                Text("Número: ${call.first}", fontSize = 22.sp, color = Color.White.copy(alpha = 0.8f))
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCall = null
                        makeDirectCall(context, call.first)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(64.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("LLAMAR AHORA", fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCall = null }) {
                    Text("CANCELAR", fontSize = 20.sp, color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun EmergencyCard(
    emoji: String,
    title: String,
    subtitle: String,
    trailing: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp),
        shape = RoundedCornerShape(32.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(2.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 44.sp)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(subtitle, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
            }
            if (trailing.isNotEmpty()) {
                Text(trailing, fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
