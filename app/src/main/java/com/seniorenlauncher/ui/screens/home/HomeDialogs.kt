package com.seniorenlauncher.ui.screens.home

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import com.seniorenlauncher.util.PermissionUtils
import kotlinx.coroutines.delay
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.seniorenlauncher.util.AppLauncher
import com.seniorenlauncher.util.InstalledApp

import androidx.compose.ui.tooling.preview.Preview
import com.seniorenlauncher.ui.theme.SeniorenLauncherTheme


@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppsSelected: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val loadedApps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = AppLauncher.getInstalledApps(context, includeIcons = true)
    }
    val installedApps = loadedApps ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isEmpty()) installedApps
        else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ELEGIR APPS", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                
                Text(
                    "Toque las apps que quiere ver en su pantalla de inicio.",
                    fontSize = 18.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Busque una app...", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                    )
                )

                Spacer(Modifier.height(16.dp))
                
                if (loadedApps == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else LazyColumn(Modifier.weight(1f)) {
                    items(filteredApps) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (pressed) 0.98f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .clickable(interactionSource = interactionSource, indication = null) { 
                                    if (isSelected) selectedPackages.remove(app.packageName)
                                    else selectedPackages.add(app.packageName)
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6))
                            )
                            Spacer(Modifier.width(12.dp))
                            if (app.icon != null) {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(app.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
                
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val cancelInteractionSource = remember { MutableInteractionSource() }
                    val cancelPressed by cancelInteractionSource.collectIsPressedAsState()
                    val cancelScale by animateFloatAsState(
                        targetValue = if (cancelPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                    )

                    OutlinedButton(
                        onClick = onDismiss, 
                        interactionSource = cancelInteractionSource,
                        modifier = Modifier.weight(1f).height(80.dp).scale(cancelScale),
                        shape = RoundedCornerShape(40.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("CANCELAR", fontWeight = FontWeight.Black)
                    }

                    val addInteractionSource = remember { MutableInteractionSource() }
                    val addPressed by addInteractionSource.collectIsPressedAsState()
                    val addScale by animateFloatAsState(
                        targetValue = if (addPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                    )

                    Button(
                        onClick = { onAppsSelected(selectedPackages.toList()) }, 
                        interactionSource = addInteractionSource,
                        modifier = Modifier.weight(1f).height(80.dp).scale(addScale),
                        enabled = selectedPackages.isNotEmpty(),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("AGREGAR", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequestDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onGrant: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF3B82F6)
                )
                Spacer(Modifier.height(24.dp))
                Text(title.uppercase(), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    description,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(Modifier.height(32.dp))

                val grantInteractionSource = remember { MutableInteractionSource() }
                val grantPressed by grantInteractionSource.collectIsPressedAsState()
                val grantScale by animateFloatAsState(
                    targetValue = if (grantPressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                )

                Button(
                    onClick = onGrant,
                    interactionSource = grantInteractionSource,
                    modifier = Modifier.fillMaxWidth().height(80.dp).scale(grantScale),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("PERMITIR", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(16.dp))

                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("CONFIGURAR MANUALMENTE", fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
