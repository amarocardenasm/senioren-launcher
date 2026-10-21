package com.seniorenlauncher.ui.screens.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Servidor DNS publico de AdGuard que bloquea publicidad, rastreadores y paginas peligrosas. */
const val ADGUARD_DNS_HOST = "dns.adguard-dns.com"

/**
 * Paso opcional del asistente: activar el "DNS privado" de Android con AdGuard.
 * Android no deja que una app lo cambie sola, asi que copiamos la direccion,
 * abrimos los ajustes y explicamos donde pegarla. Al volver, revisamos si quedo activo.
 */
@Composable
fun SafeInternetStep(onNext: () -> Unit) {
    val context = LocalContext.current
    var activeHost by remember { mutableStateOf(currentPrivateDns(context)) }
    val isActive = activeHost?.contains("adguard", ignoreCase = true) == true

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) activeHost = currentPrivateDns(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (isActive) Color(0xFF10B981) else Color(0xFF3B82F6)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Internet más seguro",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Podemos activar un filtro gratuito que bloquea publicidad engañosa, " +
                "páginas falsas y muchas estafas en todas las apps del teléfono.",
            fontSize = 22.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (isActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "✅ ¡Listo! El filtro está activo.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Cómo hacerlo:", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Toque el botón azul de abajo.\n" +
                            "2. Busque \"DNS privado\".\n" +
                            "   En Samsung: Conexiones → Más ajustes de conexión → DNS privado.\n" +
                            "3. Elija \"Nombre de host del proveedor de DNS privado\".\n" +
                            "4. Mantenga el dedo en el espacio para escribir y toque \"Pegar\".\n" +
                            "5. Toque Guardar y vuelva atrás.",
                        fontSize = 19.sp,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("La dirección que se pega es:", fontSize = 18.sp)
                    Text(
                        ADGUARD_DNS_HOST,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    copyToClipboard(context, ADGUARD_DNS_HOST)
                    openNetworkSettings(context)
                },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("COPIAR Y ABRIR AJUSTES", fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFF10B981) else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                if (isActive) "CONTINUAR" else "AHORA NO, CONTINUAR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Puede activarlo o quitarlo cuando quiera en los ajustes del teléfono.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Devuelve el servidor de DNS privado activo, o null si no hay uno configurado. */
private fun currentPrivateDns(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getLinkProperties(cm.activeNetwork)?.privateDnsServerName
    } catch (e: Exception) {
        null
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("DNS", text))
    Toast.makeText(context, "Dirección copiada", Toast.LENGTH_SHORT).show()
}

private fun openNetworkSettings(context: Context) {
    // En Android "puro" (Pixel, Motorola y otros) estos extras hacen que la
    // opcion "DNS privado" aparezca resaltada/parpadeando al abrir los ajustes.
    // Samsung usa su propio menu y los ignora: ahi sirven las instrucciones.
    val highlightKey = "private_dns_settings"
    val wireless = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
        putExtra(":settings:fragment_args_key", highlightKey)
        putExtra(":settings:show_fragment_args", android.os.Bundle().apply {
            putString(":settings:fragment_args_key", highlightKey)
        })
    }
    val intents = listOf(
        wireless,
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
}
