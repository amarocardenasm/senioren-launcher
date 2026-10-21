package com.seniorenlauncher.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorenlauncher.ui.components.ScreenHeader

/**
 * Android no deja que una app normal apague el telefono (solo el sistema puede).
 * Esta pantalla explica las dos formas que si funcionan y, en Samsung, abre el
 * ajuste para que el boton lateral vuelva a mostrar el menu de apagado.
 */
@Composable
fun PowerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScreenHeader(title = "Apagar el teléfono", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally)
            )

            PowerOption(
                number = "1",
                title = "Desde la pantalla",
                text = "Deslice el dedo desde el borde de arriba hacia abajo, dos veces. " +
                    "Arriba a la derecha aparece el botón ⏻: tóquelo y elija \"Apagar\"."
            )
            PowerOption(
                number = "2",
                title = "Con los botones",
                text = if (isSamsung) "Mantenga presionados al mismo tiempo el botón lateral y el de bajar volumen, unos 3 segundos."
                else "Mantenga presionado el botón lateral (o el lateral y el de subir volumen) unos 3 segundos."
            )

            if (isSamsung) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Para que el botón lateral apague como antes", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "En los Samsung, ese botón abre el asistente Bixby. Toque el botón de abajo y en " +
                                "\"Mantener presionado\" elija \"Menú de apagado\". Solo se hace una vez.",
                            fontSize = 19.sp,
                            lineHeight = 26.sp,
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { openSideKeySettings(context) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("ABRIR AJUSTE DEL BOTÓN", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            "Si no se abre solo: Ajustes → Funciones avanzadas → Botón lateral.",
                            fontSize = 16.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PowerOption(number: String, title: String, text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(Modifier.padding(20.dp)) {
            Box(
                Modifier.size(48.dp).background(Color(0xFFEF4444), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(number, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(text, fontSize = 19.sp, lineHeight = 26.sp, color = Color(0xFFE2E8F0), textAlign = TextAlign.Start)
            }
        }
    }
}

/** Intenta abrir el ajuste "Botón lateral" de Samsung; si no existe, abre Ajustes. */
private fun openSideKeySettings(context: android.content.Context) {
    val candidates = listOf(
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$SideKeySettingsActivity")),
        Intent().setComponent(ComponentName("com.android.settings", "com.samsung.android.settings.usefulfeature.SideKeySettingsActivity")),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
}
