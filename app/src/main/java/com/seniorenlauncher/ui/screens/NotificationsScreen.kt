package com.seniorenlauncher.ui.screens

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import android.service.notification.StatusBarNotification
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.seniorenlauncher.service.NotificationListener
import com.seniorenlauncher.ui.components.ScreenHeader

data class SingleNotification(
    val key: String,
    val title: String,
    val text: String,
    val intent: PendingIntent?,
    val timestamp: Long,
    /** Foto de quien escribe (WhatsApp, Mensajes), si la app la manda. */
    val avatar: Drawable? = null
)

data class GroupedNotification(
    val packageName: String,
    val appName: String,
    val items: List<SingleNotification>,
    val timestamp: Long,
    val appIcon: Drawable? = null
)

@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val hasPermission = remember { isNotificationServiceEnabled(context) }
    val notifications by NotificationListener.activeNotificationsFlow.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        ScreenHeader(title = "Avisos", onBack = onBack)

        if (!hasPermission) {
            PermissionRequiredScreen()
        } else {
            val grouped = remember(notifications) { groupAndFormatNotifications(context, notifications) }
            
            if (grouped.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay mensajes nuevos",
                        fontSize = 22.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(grouped) { group ->
                        val isExpanded = expandedStates[group.packageName] ?: false
                        NotificationGroupCard(
                            group = group,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedStates[group.packageName] = !isExpanded },
                            onNavigate = onNavigate
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text("BORRAR TODO", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("¿Borrar todos los avisos?", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
            text = { Text("¿Está seguro de que desea borrar todos los avisos? No se puede deshacer.", fontSize = 18.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationListener.dismissAll()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, borrar todo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("No, volver", fontSize = 18.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PermissionRequiredScreen() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Se necesita permiso",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Para mostrar aquí sus mensajes de WhatsApp, SMS y otras apps, necesitamos su permiso.",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Abrir ajustes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Cuantos mensajes se ven de cada app antes de "Ver más". */
private const val VISIBLE_PER_APP = 3

/**
 * Una tarjeta por app (como un grupo de WhatsApp): icono grande de la app
 * arriba, y debajo cada mensaje con la foto o inicial de quien lo envía.
 */
@Composable
fun NotificationGroupCard(
    group: GroupedNotification,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.appIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(group.appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.appName, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(
                        if (group.items.size == 1) "1 mensaje nuevo" else "${group.items.size} mensajes nuevos",
                        fontSize = 17.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
                IconButton(onClick = { group.items.forEach { NotificationListener.dismiss(it.key) } }) {
                    Icon(Icons.Default.Close, "Borrar los avisos de ${group.appName}", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(30.dp))
                }
            }

            val shown = if (isExpanded) group.items else group.items.take(VISIBLE_PER_APP)
            shown.forEach { item ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { handleNotificationClick(context, group.packageName, item, onNavigate) },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        SenderAvatar(item)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            if (item.title.isNotBlank()) {
                                Text(item.title, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (item.text.isNotBlank()) {
                                Text(item.text, fontSize = 19.sp, color = Color(0xFFE2E8F0), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 25.sp)
                            }
                            Text(timeAgo(item.timestamp), fontSize = 15.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            if (group.items.size > VISIBLE_PER_APP) {
                TextButton(onClick = onToggleExpand, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
                    Text(
                        if (isExpanded) "Ver menos" else "Ver ${group.items.size - VISIBLE_PER_APP} más",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SenderAvatar(item: SingleNotification) {
    if (item.avatar != null) {
        Image(
            painter = rememberAsyncImagePainter(item.avatar),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )
    } else {
        Box(
            Modifier.size(56.dp).background(Color(0xFF2563EB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(contactInitials(item.title.ifBlank { "?" }), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun timeAgo(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 1 -> "Recién"
        minutes < 60 -> "Hace $minutes min"
        minutes < 24 * 60 -> "Hace ${minutes / 60} h"
        else -> "Hace ${minutes / (24 * 60)} días"
    }
}

fun groupAndFormatNotifications(context: Context, notifications: List<StatusBarNotification>): List<GroupedNotification> {
    val pm = context.packageManager
    return notifications
        .groupBy { it.packageName }
        .map { (pkg, sbns) ->
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg.split(".").last().replaceFirstChar { it.uppercase() }
            }
            
            // El "resumen" de grupo (ej. "5 mensajes de 2 chats") se omite: mostramos cada mensaje
            val items = sbns
                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
                .ifEmpty { sbns }
                .map { sbn ->
                    SingleNotification(
                        key = sbn.key,
                        title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                        text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                        intent = sbn.notification.contentIntent,
                        timestamp = sbn.postTime,
                        avatar = try { sbn.notification.getLargeIcon()?.loadDrawable(context) } catch (e: Exception) { null }
                    )
                }.sortedByDescending { it.timestamp }

            GroupedNotification(
                packageName = pkg,
                appName = appName,
                items = items,
                timestamp = items.first().timestamp,
                appIcon = try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
            )
        }
        .sortedByDescending { it.timestamp }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}

fun handleNotificationClick(
    context: Context,
    packageName: String,
    item: SingleNotification,
    onNavigate: (String) -> Unit
) {
    // Lijsten met bekende standaard-apps van Android/Samsung/Google
    val smsPackages = listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging")
    val phonePackages = listOf("com.android.server.telecom", "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer")
    val calendarPackages = listOf("com.google.android.calendar", "com.samsung.android.calendar")

    when {
        smsPackages.contains(packageName) -> {
            NotificationListener.dismiss(item.key) 
            com.seniorenlauncher.util.AppLauncher.openMessages(context)
        }
        phonePackages.contains(packageName) -> {
            NotificationListener.dismiss(item.key)
            onNavigate("phone") // Gaat exact naar de eigen PhoneScreen
        }
        calendarPackages.contains(packageName) -> {
            NotificationListener.dismiss(item.key)
            onNavigate("calendar") // Gaat exact naar de eigen CalendarScreen
        }
        else -> {
            // Voor externe apps (zoals WhatsApp)
            try {
                if (item.intent != null) {
                    val options = android.app.ActivityOptions.makeBasic()
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        options.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    } else if (android.os.Build.VERSION.SDK_INT >= 33) {
                        @Suppress("DEPRECATION")
                        options.isPendingIntentBackgroundActivityLaunchAllowed = true
                    }
                    val fillInIntent = android.content.Intent().apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    item.intent.send(context, 0, fillInIntent, null, null, null, options.toBundle())
                } else {
                    throw Exception("Intent is null")
                }
            } catch (e: Exception) {
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                } catch (e2: Exception) {}
            }
        }
    }
}
